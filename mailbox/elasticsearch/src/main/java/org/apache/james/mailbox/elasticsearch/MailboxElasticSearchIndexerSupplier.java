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
import org.apache.james.backends.es.ElasticSearchIndexer;
import org.apache.james.backends.es.ElasticSearchIndexerSupplier;
import org.apache.james.backends.es.TypeName;
import org.elasticsearch.client.Client;

public class MailboxElasticSearchIndexerSupplier implements ElasticSearchIndexerSupplier {
    private final ElasticSearchIndexer elasticSearchIndexer;

    @Inject
    public MailboxElasticSearchIndexerSupplier(Client client,
                                               @Named("AsyncExecutor") ExecutorService executor,
                                               @Named(MailboxElasticSearchConstants.InjectionNames.MAILBOX_WRITE_ALIAS) AliasName aliasName,
                                               @Named(MailboxElasticSearchConstants.InjectionNames.MAILBOX_MAPPING) TypeName typeName) {
        this.elasticSearchIndexer = new ElasticSearchIndexer(client, executor, aliasName, typeName);
    }

    public MailboxElasticSearchIndexerSupplier(Client client,
                                               ExecutorService executor,
                                               AliasName aliasName,
                                               TypeName typeName,
                                               int batchSize) {
        this.elasticSearchIndexer = new ElasticSearchIndexer(client, executor, aliasName, typeName, batchSize);
    }

    @Override
    public ElasticSearchIndexer getElasticSearchIndexer() {
        return elasticSearchIndexer;
    }
}
