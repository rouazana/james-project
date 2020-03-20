import org.apache.james.blob.api.{BlobId, TestBlobId}
import org.scalacheck.{Arbitrary, Gen, Properties}
import org.scalacheck.Prop.forAll

case class Generation(id: Long)
case class Iteration(id: Long) {
  def next: Iteration = Iteration(id + 1)
}
case class ExternalID(id: String) // TODO

sealed trait Event {
  def getBlob: (Generation, BlobId)
}

case class Reference(externalId: ExternalID, blobId: BlobId, generation: Generation) extends Event {
  override def getBlob: (Generation, BlobId) = (generation, blobId)
}

case class Deletion(generation: Generation, reference: Reference) extends Event {
  override def getBlob: (Generation, BlobId) = reference.getBlob
}

case class Report(iteration: Iteration,
                  blobsToDelete: Set[(Generation, BlobId)]
                 )

object GC {
  def plan(events: Seq[Event], lastIteration: Iteration): Report = {
    Report(lastIteration.next, Set())
  }
}

object Generators {

  val smallInteger = Gen.choose(0L,100L)
  var current = 0;
  val generationsGen: Gen[LazyList[Generation]] = Gen.infiniteLazyList(Gen.frequency((90, Gen.const(0)), (9, Gen.const(1)), (1, Gen.const(2))))
    .map(list => list.scanLeft(0)(_ + _))
    .map(list => list.map(_.toLong).map(Generation.apply))

  val iterationGen = smallInteger.map(Iteration.apply)

  val blobIdFactory = new TestBlobId.Factory

  def blobIdGen(generation: Generation) : Gen[BlobId] = Gen.uuid.map(uuid =>
    blobIdFactory.from(s"${generation}_$uuid"))

  val externalIDGen = Gen.uuid.map(uuid => ExternalID(uuid.toString))

  def referenceGen(generation: Generation): Gen[Reference] = for {
    blobId <- blobIdGen(generation)
    externalId <- externalIDGen
  } yield Reference(externalId, blobId, generation)

  def existingReferences : Seq[Event] => Set[Reference] = _
    .foldLeft((Set[Reference](), Set[Reference]()))((acc, event) => event match {
      case deletion: Deletion => (acc._1 ++ Set(deletion.reference), acc._2)
      case reference: Reference => if (acc._1.contains(reference)) {
        acc
      } else {
        (acc._1, acc._2 ++ Set(reference))
      }
    })._2

  def deletionGen(previousEvents : Seq[Event], generation: Generation): Gen[Option[Deletion]] = {
    val remainingReferences = existingReferences(previousEvents)
    if (remainingReferences.isEmpty) {
      Gen.const(None)
    } else {
      Gen.oneOf(remainingReferences)
        .map(reference => Deletion(generation, reference))
        .map(Some(_))
    }
  }

  def duplicateReferenceGen(generation: Generation, reference: Reference): Gen[Reference] = {
    if (reference.generation == generation) {
      externalIDGen.map(id => reference.copy(externalId = id))
    } else {
      referenceGen(generation)
    }
  }

  def eventGen(previousEvents: Seq[Event], generation: Generation): Gen[Event] = for {
    greenAddEvent <- referenceGen(generation)
    addEvents = previousEvents.flatMap {
      case x: Reference => Some(x)
      case _ => None
    }
    randomAddEvent <- Gen.oneOf(addEvents)
    duplicateAddEvent <- duplicateReferenceGen(generation, randomAddEvent)
    deleteEvent <- deletionGen(previousEvents, generation)
    event <- Gen.oneOf(Seq(greenAddEvent, duplicateAddEvent) ++ deleteEvent)
  } yield event

  val eventsGen : Gen[Seq[Event]] = for {
    nbEvents <- Gen.choose(0, 100)
    generations <- generationsGen.map(_.take(nbEvents))
    startEvent <- referenceGen(Generation.apply(0))
    events <- foldM(generations, (Seq(startEvent): Seq[Event]))((previousEvents, generation) => eventGen(previousEvents, generation).map(_ +: previousEvents))
  } yield events.reverse

  def foldM[A, B](fa: LazyList[A], z: B)(f: (B, A) => Gen[B]): Gen[B] = {
    def step(in: (LazyList[A], B)): Gen[Either[(LazyList[A], B), B]] = {
      val (s, b) = in
      if (s.isEmpty)
        Gen.const(Right(b))
      else {
        f (b, s.head).map { bnext =>
          Left((s.tail, bnext))
        }
      }
    }

    Gen.tailRecM((fa, z))(step)
  }
}

object GCPropertiesTest extends Properties("GC") {
  implicit val arbEvents = Arbitrary(Generators.eventsGen)
//  Generators.eventsGen().sample.foreach(_.foreach(println))
  property("2.1. GC should not delete data being referenced by a pending process or still referenced") = forAll {
    events: Seq[Event] => {
      val (creationEvents, deletionEvents) = events.partition {
        case _ : Reference => true
        case _ : Deletion => false
      }
      val createdBlobs = creationEvents.map(_.getBlob._2).toSet
      val deletedBlobs = deletionEvents.map(_.getBlob._2).toSet
      val remainingBlobs = createdBlobs -- deletedBlobs
      val plannedDeletions = GC.plan(events, Iteration(0)).blobsToDelete.map(_._2)
      remainingBlobs.intersect(plannedDeletions).isEmpty
    }
  }
}
