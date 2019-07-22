/** **************************************************************
  * Licensed to the Apache Software Foundation (ASF) under one   *
  * or more contributor license agreements.  See the NOTICE file *
  * distributed with this work for additional information        *
  * regarding copyright ownership.  The ASF licenses this file   *
  * to you under the Apache License, Version 2.0 (the            *
  * "License"); you may not use this file except in compliance   *
  * with the License.  You may obtain a copy of the License at   *
  * *
  * http://www.apache.org/licenses/LICENSE-2.0                 *
  * *
  * Unless required by applicable law or agreed to in writing,   *
  * software distributed under the License is distributed on an  *
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
  * KIND, either express or implied.  See the License for the    *
  * specific language governing permissions and limitations      *
  * under the License.                                           *
  * ***************************************************************/
package org.apache.james.task.eventsourcing

import java.io.Closeable
import java.util

import com.google.common.annotations.VisibleForTesting
import javax.inject.Inject
import org.apache.james.eventsourcing.{AggregateId, Subscriber}
import org.apache.james.eventsourcing.eventstore.{EventStore, History}
import org.apache.james.task._
import org.apache.james.task.eventsourcing.TaskCommand._

import scala.annotation.tailrec

class EventSourcingTaskManager @Inject @VisibleForTesting private[eventsourcing](val eventStore: EventStore) extends TaskManager with Closeable {

  private val executionDetailsProjection = new TaskExecutionDetailsProjection
  private val recentTasksProjection = new RecentTasksProjection()
  private val workQueue: WorkQueue = WorkQueue.builder().worker(new SerialTaskManagerWorker)
  private val delayBetweenPollingInMs = 500

  private def workDispatcher: Subscriber = {
    case Created(aggregateId, _, task) =>
      val taskWithId = new TaskWithId(aggregateId.taskId, task)
      workQueue.submit(taskWithId, new WorkerStatusListener(taskWithId.getId, eventSourcingSystem))
    case CancelRequested(aggregateId, _) =>
      workQueue.cancel(aggregateId.taskId)
    case _ =>
  }

  import scala.collection.JavaConverters._

  private val loadHistory: AggregateId => History = eventStore.getEventsOfAggregate _
  private val eventSourcingSystem = ScalaEventSourcingSystem(
    handlers = Set(
      new CreateCommandHandler(loadHistory),
      new StartCommandHandler(loadHistory),
      new RequestCancelCommandHandler(loadHistory),
      new CompleteCommandHandler(loadHistory),
      new CancelCommandHandler(loadHistory),
      new FailCommandHandler(loadHistory)),
    subscribers = Set(
      executionDetailsProjection.asSubscriber,
      workDispatcher,
      recentTasksProjection.asSubscriber),
    eventStore = eventStore)

  override def submit(task: Task): TaskId = {
    val taskId = TaskId.generateTaskId
    val command = Create(taskId, task)
    eventSourcingSystem.dispatch(command)
    taskId
  }

  override def getExecutionDetails(id: TaskId): TaskExecutionDetails = executionDetailsProjection.load(id)
    .getOrElse(throw new TaskNotFoundException())

  override def list: util.List[TaskExecutionDetails] = listScala.asJava

  override def list(status: TaskManager.Status): util.List[TaskExecutionDetails] = listScala
    .filter(details => details.getStatus == status)
    .asJava

  private def listScala: List[TaskExecutionDetails] = recentTasksProjection
    .list()
    .flatMap(executionDetailsProjection.load)

  override def cancel(id: TaskId): Unit = {
    val command = RequestCancel(id)
    eventSourcingSystem.dispatch(command)
  }

  @tailrec
  override final def await(id: TaskId): TaskExecutionDetails = {
    val details = getExecutionDetails(id)
    if (details.getStatus.isFinished) {
      details
    } else {
      Thread.sleep(delayBetweenPollingInMs)
      await(id)
    }
  }

  override def close(): Unit = {
    workQueue.close()
  }
}