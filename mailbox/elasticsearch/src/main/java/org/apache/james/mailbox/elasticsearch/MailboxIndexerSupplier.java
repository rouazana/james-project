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
package org.apache.james.mailbox.elasticsearch;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.james.backends.es.AliasName;
import org.apache.james.backends.es.Indexer;
import org.apache.james.backends.es.IndexerSupplier;
import org.apache.james.backends.es.TypeName;
import org.elasticsearch.client.Client;

public class MailboxIndexerSupplier implements IndexerSupplier {
    private final Indexer indexer;

    @Inject
    public MailboxIndexerSupplier(Client client,
                                  @Named("AsyncExecutor") ExecutorService executor,
                                  @Named(MailboxElasticSearchConstants.InjectionNames.MAILBOX_WRITE_ALIAS) AliasName aliasName,
                                  @Named(MailboxElasticSearchConstants.InjectionNames.MAILBOX_MAPPING) TypeName typeName) {
        this.indexer = new Indexer(client, executor, aliasName, typeName);
    }

    public MailboxIndexerSupplier(Client client,
                                  ExecutorService executor,
                                  AliasName aliasName,
                                  TypeName typeName,
                                  int batchSize) {
        this.indexer = new Indexer(client, executor, aliasName, typeName, batchSize);
    }

    @Override
    public Indexer getIndexer() {
        return indexer;
    }
}
