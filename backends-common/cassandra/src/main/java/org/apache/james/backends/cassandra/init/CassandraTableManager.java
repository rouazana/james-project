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

package org.apache.james.backends.cassandra.init;

import javax.inject.Inject;

import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.components.CassandraTable;
import org.apache.james.backends.cassandra.components.CassandraTable.InitializationStatus;
import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class CassandraTableManager {

    private final CqlSession session;
    private final CassandraModule module;

    @Inject
    public CassandraTableManager(CassandraModule module, CqlSession session) {
        this.session = session;
        this.module = module;
    }

    public InitializationStatus initializeTables() {
        KeyspaceMetadata keyspaceMetadata = session.getKeyspace()
                .flatMap(keyspace -> session.getMetadata().getKeyspace(keyspace))
                .orElseThrow(() -> new RuntimeException("TODO"));

        return module.moduleTables()
                .stream()
                .map(table -> table.initialize(keyspaceMetadata, session))
                .reduce((left, right) -> left.reduce(right))
                .orElse(InitializationStatus.ALREADY_DONE);
    }

    public void clearAllTables() {
        CassandraAsyncExecutor executor = new CassandraAsyncExecutor(session);
        Flux.fromIterable(module.moduleTables())
                .publishOn(Schedulers.elastic())
                .map(CassandraTable::getName)
                .flatMap(name -> truncate(executor, name))
                .then()
                .block();
    }

    private Mono<Void> truncate(CassandraAsyncExecutor executor, String name) {
        return executor.execute(
                QueryBuilder.selectFrom(name)
                        .all()
                        .limit(1)
                        /*FIXME?.setFetchSize(1)*/.build())
                .filter(AsyncResultSet::wasApplied) //FIXME really the same than !isExhausted?
                .flatMap(ignored -> executor.executeVoid(QueryBuilder.truncate(name).build()));
    }
}
