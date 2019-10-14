/**
 * *************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 * http://www.apache.org/licenses/LICENSE-2.0                   *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ***************************************************************/

package org.apache.james.rrt.cassandra.migration;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.apache.james.server.task.json.JsonTaskAdditionalInformationsSerializer;
import org.apache.james.server.task.json.JsonTaskSerializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

class MappingsSourcesMigrationTaskSerializationTest {
    private static final MappingsSourcesMigration MIGRATION = mock(MappingsSourcesMigration.class);
    private static final MappingsSourcesMigration.MappingsSourcesMigrationTask TASK = new MappingsSourcesMigration.MappingsSourcesMigrationTask(MIGRATION);
    private static final String SERIALIZED_TASK = "{\"type\": \"mappingsSourcesMigration\"}";
    private static final MappingsSourcesMigration.AdditionalInformation DETAILS = new MappingsSourcesMigration.AdditionalInformation(42L, 10);
    private static final String SERIALIZED_ADDITIONAL_INFORMATION = "{\"type\": \"mappingsSourcesMigration\", \"successfulMappingsCount\":42,\"errorMappinsCount\":10}";

    private static final JsonTaskSerializer TASK_SERIALIZER = new JsonTaskSerializer(MappingsSourcesMigrationTaskDTO.MODULE.apply(MIGRATION));
    private static final JsonTaskAdditionalInformationsSerializer JSON_TASK_ADDITIONAL_INFORMATIONS_SERIALIZER = new JsonTaskAdditionalInformationsSerializer(MappingsSourcesMigrationTaskAdditionalInformationDTO.serializationModule(MappingsSourcesMigration.TYPE));

    @Test
    void taskShouldBeSerializable() throws JsonProcessingException {
        assertThatJson(TASK_SERIALIZER.serialize(TASK))
            .isEqualTo(SERIALIZED_TASK);
    }

    @Test
    void taskShouldBeDeserializable() throws IOException {
        assertThat(TASK_SERIALIZER.deserialize(SERIALIZED_TASK))
            .isEqualToComparingFieldByField(TASK);
    }

    @Test
    void additionalInformationShouldBeSerializable() throws JsonProcessingException {
        assertThatJson(JSON_TASK_ADDITIONAL_INFORMATIONS_SERIALIZER.serialize(DETAILS)).isEqualTo(SERIALIZED_ADDITIONAL_INFORMATION);
    }

    @Test
    void additonalInformationShouldBeDeserializable() throws IOException {
        assertThat(JSON_TASK_ADDITIONAL_INFORMATIONS_SERIALIZER.deserialize(SERIALIZED_ADDITIONAL_INFORMATION))
            .isEqualToComparingFieldByField(DETAILS);
    }
}