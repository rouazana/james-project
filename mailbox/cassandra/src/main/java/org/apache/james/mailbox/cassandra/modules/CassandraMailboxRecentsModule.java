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

package org.apache.james.mailbox.cassandra.modules;

import static com.datastax.driver.core.DataType.bigint;
import static com.datastax.driver.core.DataType.timeuuid;

import java.util.Collections;
import java.util.List;

import org.apache.james.backends.cassandra.components.CassandraIndex;
import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.components.CassandraTable;
import org.apache.james.backends.cassandra.components.CassandraType;
import org.apache.james.mailbox.cassandra.table.CassandraMailboxRecentsTable;
import org.apache.james.mailbox.cassandra.table.CassandraSubscriptionTable;

import com.datastax.driver.core.schemabuilder.SchemaBuilder;
import com.google.common.collect.ImmutableList;

public class CassandraMailboxRecentsModule implements CassandraModule {

    private final List<CassandraTable> tables;
    private final List<CassandraIndex> index;
    private final List<CassandraType> types;

    public CassandraMailboxRecentsModule() {
        tables = Collections.singletonList(
            new CassandraTable(CassandraMailboxRecentsTable.TABLE_NAME,
                SchemaBuilder.createTable(CassandraMailboxRecentsTable.TABLE_NAME)
                    .ifNotExists()
                    .addPartitionKey(CassandraMailboxRecentsTable.MAILBOX_ID, timeuuid())
                    .addClusteringColumn(CassandraMailboxRecentsTable.RECENT_MESSAGE_UID, bigint())));
        index = ImmutableList.of(            new CassandraIndex(
                SchemaBuilder.createIndex(CassandraIndex.INDEX_PREFIX + CassandraMailboxRecentsTable.RECENT_MESSAGE_UID)
                .ifNotExists()
                .onTable(CassandraMailboxRecentsTable.TABLE_NAME)
                .andColumn(CassandraMailboxRecentsTable.RECENT_MESSAGE_UID)));
//);
        types = Collections.emptyList();
    }

    @Override
    public List<CassandraTable> moduleTables() {
        return tables;
    }

    @Override
    public List<CassandraIndex> moduleIndex() {
        return index;
    }

    @Override
    public List<CassandraType> moduleTypes() {
        return types;
    }
}
