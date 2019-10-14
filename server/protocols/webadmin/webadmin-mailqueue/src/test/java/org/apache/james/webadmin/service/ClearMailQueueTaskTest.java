/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 * http://www.apache.org/licenses/LICENSE-2.0                   *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 * ***************************************************************/

package org.apache.james.webadmin.service;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import org.apache.james.queue.api.MailQueueFactory;
import org.apache.james.queue.api.ManageableMailQueue;
import org.apache.james.server.task.json.JsonTaskAdditionalInformationsSerializer;
import org.apache.james.server.task.json.JsonTaskSerializer;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;

class ClearMailQueueTaskTest {

    private static final String SERIALIZED = "{\"type\": \"clear-mail-queue\", \"queue\": \"anyQueue\"}";
    private static final String QUEUE_NAME = "anyQueue";
    private static final long INITIAL_COUNT = 0L;
    private static final long REMAINING_COUNT = 10L;
    private JsonTaskAdditionalInformationsSerializer jsonAdditionalInformationSerializer = new JsonTaskAdditionalInformationsSerializer(
        ClearMailQueueTaskAdditionalInformationDTO.SERIALIZATION_MODULE);
    private static final String SERIALIZED_TASK_ADDITIONAL_INFORMATION = "{\"type\": \"clear-mail-queue\", \"mailQueueName\":\"anyQueue\",\"initialCount\":0,\"remainingCount\":10}";

    @Test
    void taskShouldBeSerializable() throws Exception {
        MailQueueFactory<ManageableMailQueue> mailQueueFactory = mock(MailQueueFactory.class);
        ManageableMailQueue mockedQueue = mock(ManageableMailQueue.class);
        when(mockedQueue.getName()).thenReturn(QUEUE_NAME);
        when(mailQueueFactory.getQueue(anyString())).thenAnswer(arg -> Optional.of(mockedQueue));
        JsonTaskSerializer testee = new JsonTaskSerializer(ClearMailQueueTaskDTO.module(mailQueueFactory));

        ManageableMailQueue queue = mailQueueFactory.getQueue(QUEUE_NAME).get();
        ClearMailQueueTask task = new ClearMailQueueTask(queue);
        JsonAssertions.assertThatJson(testee.serialize(task)).isEqualTo(SERIALIZED);
    }

    @Test
    void taskShouldBeDeserializable() throws Exception {
        MailQueueFactory<ManageableMailQueue> mailQueueFactory = mock(MailQueueFactory.class);
        ManageableMailQueue mockedQueue = mock(ManageableMailQueue.class);
        when(mockedQueue.getName()).thenReturn(QUEUE_NAME);
        when(mailQueueFactory.getQueue(anyString())).thenAnswer(arg -> Optional.of(mockedQueue));
        JsonTaskSerializer testee = new JsonTaskSerializer(ClearMailQueueTaskDTO.module(mailQueueFactory));

        ManageableMailQueue queue = mailQueueFactory.getQueue(QUEUE_NAME).get();
        ClearMailQueueTask task = new ClearMailQueueTask(queue);
        assertThat(testee.deserialize(SERIALIZED)).isEqualToIgnoringGivenFields(task, "additionalInformation");
    }

    @Test
    void taskShouldThrowWhenDeserializeAnUnknownQueue() throws Exception {
        MailQueueFactory<ManageableMailQueue> mailQueueFactory = mock(MailQueueFactory.class);
        when(mailQueueFactory.getQueue(anyString())).thenReturn(Optional.empty());
        JsonTaskSerializer testee = new JsonTaskSerializer(ClearMailQueueTaskDTO.module(mailQueueFactory));

        String serializedJson = "{\"type\": \"clear-mail-queue\", \"queue\": \"anyQueue\"}";
        assertThatThrownBy(() -> testee.deserialize(serializedJson))
            .isInstanceOf(ClearMailQueueTask.UnknownSerializedQueue.class);
    }

    @Test
    void additionalInformationShouldBeSerializable() throws JsonProcessingException {
        ClearMailQueueTask.AdditionalInformation details = new ClearMailQueueTask.AdditionalInformation(QUEUE_NAME, INITIAL_COUNT, REMAINING_COUNT);
        assertThatJson(jsonAdditionalInformationSerializer.serialize(details)).isEqualTo(SERIALIZED_TASK_ADDITIONAL_INFORMATION);
    }

    @Test
    void additionalInformationShouldBeDeserializable() throws IOException {
        ClearMailQueueTask.AdditionalInformation details = new ClearMailQueueTask.AdditionalInformation(QUEUE_NAME, INITIAL_COUNT, REMAINING_COUNT);
        assertThat(jsonAdditionalInformationSerializer.deserialize(SERIALIZED_TASK_ADDITIONAL_INFORMATION))
            .isEqualToComparingFieldByField(details);
    }
}
