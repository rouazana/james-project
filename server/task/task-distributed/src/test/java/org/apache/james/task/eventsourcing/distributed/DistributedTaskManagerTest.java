/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.task.eventsourcing.distributed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.CassandraClusterExtension;
import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.init.CassandraZonedDateTimeModule;
import org.apache.james.backends.cassandra.versions.CassandraSchemaVersionModule;
import org.apache.james.backends.rabbitmq.RabbitMQExtension;
import org.apache.james.backends.rabbitmq.SimpleConnectionPool;
import org.apache.james.eventsourcing.EventSourcingSystem;
import org.apache.james.eventsourcing.eventstore.EventStore;
import org.apache.james.eventsourcing.eventstore.cassandra.CassandraEventStoreExtension;
import org.apache.james.eventsourcing.eventstore.cassandra.CassandraEventStoreModule;
import org.apache.james.eventsourcing.eventstore.cassandra.JsonEventSerializer;
import org.apache.james.eventsourcing.eventstore.cassandra.dto.EventDTOModule;
import org.apache.james.server.task.json.JsonTaskAdditionalInformationsSerializer;
import org.apache.james.server.task.json.JsonTaskSerializer;
import org.apache.james.server.task.json.dto.MemoryReferenceTaskStore;
import org.apache.james.server.task.json.dto.MemoryReferenceWithCounterTaskAdditionalInformationDTO;
import org.apache.james.server.task.json.dto.MemoryReferenceWithCounterTaskStore;
import org.apache.james.server.task.json.dto.TestTaskDTOModules;
import org.apache.james.task.CompletedTask;
import org.apache.james.task.CountDownLatchExtension;
import org.apache.james.task.Hostname;
import org.apache.james.task.MemoryReferenceTask;
import org.apache.james.task.Task;
import org.apache.james.task.TaskExecutionDetails;
import org.apache.james.task.TaskId;
import org.apache.james.task.TaskManager;
import org.apache.james.task.TaskManagerContract;
import org.apache.james.task.WorkQueue;
import org.apache.james.task.eventsourcing.EventSourcingTaskManager;
import org.apache.james.task.eventsourcing.TaskExecutionDetailsProjection;
import org.apache.james.task.eventsourcing.WorkQueueSupplier;
import org.apache.james.task.eventsourcing.cassandra.CassandraTaskExecutionDetailsProjection;
import org.apache.james.task.eventsourcing.cassandra.CassandraTaskExecutionDetailsProjectionDAO;
import org.apache.james.task.eventsourcing.cassandra.CassandraTaskExecutionDetailsProjectionModule;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.steveash.guavate.Guavate;

@ExtendWith(CountDownLatchExtension.class)
class DistributedTaskManagerTest implements TaskManagerContract {

    private static class TrackedRabbitMQWorkQueueSupplier implements WorkQueueSupplier {
        private final List<RabbitMQWorkQueue> workQueues;
        private final RabbitMQWorkQueueSupplier supplier;

        TrackedRabbitMQWorkQueueSupplier(SimpleConnectionPool rabbitConnectionPool, JsonTaskSerializer taskSerializer) {
            workQueues = new ArrayList<>();
            supplier = new RabbitMQWorkQueueSupplier(rabbitConnectionPool, taskSerializer);
        }

        @Override
        public WorkQueue apply(EventSourcingSystem eventSourcingSystem) {
            RabbitMQWorkQueue workQueue = supplier.apply(eventSourcingSystem);
            workQueues.add(workQueue);
            return workQueue;
        }

        void stopWorkQueues() {
            workQueues.forEach(RabbitMQWorkQueue::close);
            workQueues.clear();
        }
    }

    private static final JsonTaskSerializer TASK_SERIALIZER = new JsonTaskSerializer(
        TestTaskDTOModules.COMPLETED_TASK_MODULE,
        TestTaskDTOModules.FAILED_TASK_MODULE,
        TestTaskDTOModules.THROWING_TASK_MODULE,
        TestTaskDTOModules.MEMORY_REFERENCE_TASK_MODULE.apply(new MemoryReferenceTaskStore()),
        TestTaskDTOModules.MEMORY_REFERENCE_WITH_COUNTER_TASK_MODULE.apply(new MemoryReferenceWithCounterTaskStore())
        );

    private static final JsonTaskAdditionalInformationsSerializer jsonTaskAdditionalInformationsSerializer = new JsonTaskAdditionalInformationsSerializer(MemoryReferenceWithCounterTaskAdditionalInformationDTO.SERIALIZATION_MODULE);

    private static final Hostname HOSTNAME = new Hostname("foo");
    private static final Hostname HOSTNAME_2 = new Hostname("bar");
    private static final Set<EventDTOModule<?, ?>> MODULES = TasksSerializationModule.MODULES.apply(TASK_SERIALIZER, jsonTaskAdditionalInformationsSerializer).stream().collect(Guavate.toImmutableSet());
    private static final JsonEventSerializer EVENT_SERIALIZER = new JsonEventSerializer(MODULES);


    static CassandraClusterExtension cassandraCluster = new CassandraClusterExtension(
        CassandraModule.aggregateModules(
            CassandraSchemaVersionModule.MODULE,
            CassandraEventStoreModule.MODULE,
            CassandraZonedDateTimeModule.MODULE,
            CassandraTaskExecutionDetailsProjectionModule.MODULE()));

    @RegisterExtension
    static RabbitMQExtension rabbitMQExtension = RabbitMQExtension.singletonRabbitMQ();

    @RegisterExtension
    static CassandraEventStoreExtension eventStoreExtension = new CassandraEventStoreExtension(cassandraCluster, MODULES);

    private final CassandraCluster cassandra = cassandraCluster.getCassandraCluster();
    private final CassandraTaskExecutionDetailsProjectionDAO cassandraTaskExecutionDetailsProjectionDAO = new CassandraTaskExecutionDetailsProjectionDAO(cassandra.getConf(), cassandra.getTypesProvider(), jsonTaskAdditionalInformationsSerializer);
    private final TaskExecutionDetailsProjection executionDetailsProjection = new CassandraTaskExecutionDetailsProjection(cassandraTaskExecutionDetailsProjectionDAO);

    private TrackedRabbitMQWorkQueueSupplier workQueueSupplier;
    private EventStore eventStore;
    private List<RabbitMQTerminationSubscriber> terminationSubscribers;

    @BeforeEach
    void setUp(EventStore eventStore) {
        workQueueSupplier = new TrackedRabbitMQWorkQueueSupplier(rabbitMQExtension.getRabbitConnectionPool(), TASK_SERIALIZER);
        this.eventStore = eventStore;
        terminationSubscribers = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        terminationSubscribers.forEach(RabbitMQTerminationSubscriber::close);
        workQueueSupplier.stopWorkQueues();
    }

    public EventSourcingTaskManager taskManager() {
        return taskManager(HOSTNAME);
    }

    private EventSourcingTaskManager taskManager(Hostname hostname) {
        RabbitMQTerminationSubscriber terminationSubscriber = new RabbitMQTerminationSubscriber(rabbitMQExtension.getRabbitConnectionPool(), EVENT_SERIALIZER);
        terminationSubscribers.add(terminationSubscriber);
        terminationSubscriber.start();
        return new EventSourcingTaskManager(workQueueSupplier, eventStore, executionDetailsProjection, hostname, terminationSubscriber);
    }

    @Test
    void givenOneEventStoreTwoEventTaskManagersShareTheSameEvents() {
        try (EventSourcingTaskManager taskManager1 = taskManager();
             EventSourcingTaskManager taskManager2 = taskManager(HOSTNAME_2)) {
            TaskId taskId = taskManager1.submit(new CompletedTask());
            Awaitility.await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(100L, TimeUnit.MILLISECONDS)
                .until(() -> taskManager1.await(taskId, TIMEOUT).getStatus() == TaskManager.Status.COMPLETED);

            TaskExecutionDetails detailsFromTaskManager1 = taskManager1.getExecutionDetails(taskId);
            TaskExecutionDetails detailsFromTaskManager2 = taskManager2.getExecutionDetails(taskId);
            assertThat(detailsFromTaskManager1).isEqualTo(detailsFromTaskManager2);
        }
    }

    @Test
    void givenTwoTaskManagersAndTwoTaskOnlyOneTaskShouldRunAtTheSameTime() throws InterruptedException {
        CountDownLatch waitingForFirstTaskLatch = new CountDownLatch(1);

        try (EventSourcingTaskManager taskManager1 = taskManager();
             EventSourcingTaskManager taskManager2 = taskManager(HOSTNAME_2)) {

            taskManager1.submit(new MemoryReferenceTask(() -> {
                waitingForFirstTaskLatch.await();
                return Task.Result.COMPLETED;
            }));
            TaskId waitingTaskId = taskManager1.submit(new CompletedTask());

            awaitUntilTaskHasStatus(waitingTaskId, TaskManager.Status.WAITING, taskManager2);
            waitingForFirstTaskLatch.countDown();

            Awaitility.await()
                .atMost(Duration.ONE_SECOND)
                .pollInterval(100L, TimeUnit.MILLISECONDS)
                .until(() -> taskManager1.await(waitingTaskId, TIMEOUT).getStatus() == TaskManager.Status.COMPLETED);
        }
    }

    @Test
    void givenTwoTaskManagerATaskSubmittedOnOneCouldBeRunOnTheOther() throws InterruptedException {
        try (EventSourcingTaskManager taskManager1 = taskManager()) {
            Thread.sleep(100);
            try (EventSourcingTaskManager taskManager2 = taskManager(HOSTNAME_2)) {

                TaskId taskId = taskManager2.submit(new CompletedTask());

                Awaitility.await()
                    .atMost(Duration.ONE_SECOND)
                    .pollInterval(100L, TimeUnit.MILLISECONDS)
                    .until(() -> taskManager1.await(taskId, TIMEOUT).getStatus() == TaskManager.Status.COMPLETED);

                TaskExecutionDetails executionDetails = taskManager2.getExecutionDetails(taskId);
                assertThat(executionDetails.getSubmittedNode()).isEqualTo(HOSTNAME_2);
                assertThat(executionDetails.getRanNode()).contains(HOSTNAME);
            }
        }
    }

    @Test
    void givenTwoTaskManagerATaskRunningOnOneShouldBeCancellableFromTheOtherOne(CountDownLatch countDownLatch) {
        TaskManager taskManager1 = taskManager(HOSTNAME);
        TaskManager taskManager2 = taskManager(HOSTNAME_2);
        TaskId id = taskManager1.submit(new MemoryReferenceTask(() -> {
            countDownLatch.await();
            return Task.Result.COMPLETED;
        }));

        awaitUntilTaskHasStatus(id, TaskManager.Status.IN_PROGRESS, taskManager1);
        Hostname runningNode = taskManager1.getExecutionDetails(id).getRanNode().get();

        Pair<Hostname, TaskManager> remoteTaskManager = getOtherTaskManager(runningNode, Pair.of(HOSTNAME, taskManager1), Pair.of(HOSTNAME_2, taskManager2));
        remoteTaskManager.getValue().cancel(id);

        awaitAtMostFiveSeconds.untilAsserted(() ->
            assertThat(taskManager1.getExecutionDetails(id).getStatus())
                .isIn(TaskManager.Status.CANCELLED, TaskManager.Status.CANCEL_REQUESTED));

        countDownLatch.countDown();

        awaitUntilTaskHasStatus(id, TaskManager.Status.CANCELLED, taskManager1);
        assertThat(taskManager1.getExecutionDetails(id).getStatus())
            .isEqualTo(TaskManager.Status.CANCELLED);

        assertThat(taskManager1.getExecutionDetails(id).getCancelRequestedNode())
            .contains(remoteTaskManager.getKey());
    }

    @Test
    void givenTwoTaskManagersATaskRunningOnOneShouldBeWaitableFromTheOtherOne() throws TaskManager.ReachedTimeoutException {
        TaskManager taskManager1 = taskManager(HOSTNAME);
        TaskManager taskManager2 = taskManager(HOSTNAME_2);
        TaskId id = taskManager1.submit(new MemoryReferenceTask(() -> {
            Thread.sleep(250);
            return Task.Result.COMPLETED;
        }));

        awaitUntilTaskHasStatus(id, TaskManager.Status.IN_PROGRESS, taskManager1);
        Hostname runningNode = taskManager1.getExecutionDetails(id).getRanNode().get();

        TaskManager remoteTaskManager = getOtherTaskManager(runningNode, Pair.of(HOSTNAME, taskManager1), Pair.of(HOSTNAME_2, taskManager2)).getValue();

        remoteTaskManager.await(id, TIMEOUT);
        assertThat(taskManager1.getExecutionDetails(id).getStatus())
            .isEqualTo(TaskManager.Status.COMPLETED);
    }

    private Pair<Hostname, TaskManager> getOtherTaskManager(Hostname node, Pair<Hostname, TaskManager> taskManager1, Pair<Hostname, TaskManager> taskManager2) {
        if (node.equals(taskManager1.getKey())) {
            return taskManager2;
        } else {
            return taskManager1;
        }
    }

    @Test
    void givenTwoTaskManagerAndATaskRanPerTaskManagerListingThemOnEachShouldShowBothTasks() {
        try (EventSourcingTaskManager taskManager1 = taskManager();
             EventSourcingTaskManager taskManager2 = taskManager(HOSTNAME_2)) {

            TaskId taskId1 = taskManager1.submit(new CompletedTask());
            TaskId taskId2 = taskManager2.submit(new CompletedTask());

            Awaitility.await()
                .atMost(Duration.ONE_SECOND)
                .pollInterval(100L, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    List<TaskExecutionDetails> listOnTaskManager1 = taskManager1.list();
                    List<TaskExecutionDetails> listOnTaskManager2 = taskManager2.list();

                    assertThat(listOnTaskManager1)
                        .hasSize(2)
                        .isEqualTo(listOnTaskManager2)
                        .allSatisfy(taskExecutionDetails -> assertThat(taskExecutionDetails.getStatus()).isEqualTo(TaskManager.Status.COMPLETED))
                        .extracting(TaskExecutionDetails::getTaskId)
                        .containsExactlyInAnyOrder(taskId1, taskId2);
                });
        }
    }
}
