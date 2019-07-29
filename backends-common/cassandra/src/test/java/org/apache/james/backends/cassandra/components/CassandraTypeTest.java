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
package org.apache.james.backends.cassandra.components;

import static org.apache.james.backends.cassandra.components.CassandraType.InitializationStatus.ALREADY_DONE;
import static org.apache.james.backends.cassandra.components.CassandraType.InitializationStatus.FULL;
import static org.apache.james.backends.cassandra.components.CassandraType.InitializationStatus.PARTIAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.james.backends.cassandra.components.CassandraType.InitializationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import com.datastax.oss.driver.api.core.type.DataTypes;
import com.datastax.oss.driver.api.core.type.UserDefinedType;
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder;
import com.datastax.oss.driver.api.querybuilder.schema.CreateType;

import nl.jqno.equalsverifier.EqualsVerifier;

class CassandraTypeTest {
    private static final String NAME = "typeName";
    private static final CreateType STATEMENT = SchemaBuilder.createType(NAME).withField("type", DataTypes.BOOLEAN);
    private static final CassandraType TYPE = new CassandraType(NAME, STATEMENT);

    @Test
    void shouldRespectBeanContract() {
        EqualsVerifier.forClass(CassandraType.class)
                .withPrefabValues(CreateType.class,
                    SchemaBuilder.createType("name1").withField("type1", DataTypes.BOOLEAN),
                    SchemaBuilder.createType("name2").withField("type2", DataTypes.BOOLEAN))
                .verify();
    }

    @Test
    void initializeShouldExecuteCreateStatementAndReturnFullWhenTypeDoesNotExist() {
        KeyspaceMetadata keyspace = mock(KeyspaceMetadata.class);
        when(keyspace.getUserDefinedType(NAME)).thenReturn(null);
        CqlSession session = mock(CqlSession.class);

        assertThat(TYPE.initialize(keyspace, session))
                .isEqualByComparingTo(FULL);

        verify(keyspace).getUserDefinedType(NAME);
        verify(session).execute(STATEMENT.build());
    }

    @Test
    void initializeShouldReturnAlreadyDoneWhenTypeExists() {
        KeyspaceMetadata keyspace = mock(KeyspaceMetadata.class);
        when(keyspace.getUserDefinedType(NAME)).thenReturn(Optional.of(mock(UserDefinedType.class)));
        CqlSession session = mock(CqlSession.class);

        assertThat(TYPE.initialize(keyspace, session))
                .isEqualByComparingTo(ALREADY_DONE);

        verify(keyspace).getUserDefinedType(NAME);
        verify(session, never()).execute(STATEMENT.build());
    }

    @ParameterizedTest
    @MethodSource
    void initializationStatusReduceShouldFallIntoTheRightState(InitializationStatus left, InitializationStatus right, InitializationStatus expectedResult) {
        assertThat(left.reduce(right)).isEqualByComparingTo(expectedResult);
    }

    static Stream<Arguments> initializationStatusReduceShouldFallIntoTheRightState() {
        return Stream.of(
                Arguments.of(ALREADY_DONE, ALREADY_DONE, ALREADY_DONE),
                Arguments.of(ALREADY_DONE, PARTIAL, PARTIAL),
                Arguments.of(ALREADY_DONE, FULL, PARTIAL),
                Arguments.of(PARTIAL, PARTIAL, PARTIAL),
                Arguments.of(PARTIAL, PARTIAL, PARTIAL),
                Arguments.of(PARTIAL, FULL, PARTIAL),
                Arguments.of(FULL, ALREADY_DONE, PARTIAL),
                Arguments.of(FULL, PARTIAL, PARTIAL),
                Arguments.of(FULL, FULL, FULL)
        );
    }
}