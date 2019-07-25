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
package org.apache.james.backends.cassandra;

import java.net.InetSocketAddress;

import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.init.CassandraTableManager;
import org.apache.james.backends.cassandra.init.CassandraTypesProvider;
import org.apache.james.util.Host;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder;
import com.datastax.oss.driver.api.querybuilder.schema.CreateKeyspace;

public final class CassandraCluster implements AutoCloseable {
    public static final CqlIdentifier KEYSPACE = CqlIdentifier.fromCql("testing");

    private final CassandraModule module;
    private CqlSession session;
    private CassandraTypesProvider typesProvider;

    public static CassandraCluster create(CassandraModule module, Host host) {
        return new CassandraCluster(module, host);
    }

    private CassandraCluster(CassandraModule module, Host host) throws RuntimeException {
        this.module = module;
        try {
            CqlSession.builder()
                    .addContactPoint(new InetSocketAddress(host.getHostName(), host.getPort()))
                    .withLocalDatacenter("datacenter1") // be careful I don't know wky but it seems mandatory
                    .build()
                    .execute(SchemaBuilder.createKeyspace(KEYSPACE)
                .withSimpleStrategy(1)
                .withDurableWrites(false).build());
            CqlSession session = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(host.getHostName(), host.getPort()))
                .withKeyspace(KEYSPACE)
                .withLocalDatacenter("datacenter1") // be careful I don't know wky but it seems mandatory
                .build();
//            CreateKeyspace createKeyspace = SchemaBuilder.createKeyspace(KEYSPACE)
//                .withSimpleStrategy(1)
//                .withDurableWrites(false);
//            session.execute(createKeyspace.build());
            typesProvider = new CassandraTypesProvider(module, session);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public CqlSession getConf() {
        return session;
    }

    public CassandraTypesProvider getTypesProvider() {
        return typesProvider;
    }

    @Override
    public void close() {
        if (!session.isClosed()) {
            clearTables();
            closeCluster();
        }
    }

    public void closeCluster() {
        session.closeAsync();
    }

    public void clearTables() {
        new CassandraTableManager(module, session).clearAllTables();
    }
}
