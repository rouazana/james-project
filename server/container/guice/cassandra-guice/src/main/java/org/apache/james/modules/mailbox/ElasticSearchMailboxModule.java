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

package org.apache.james.modules.mailbox;

import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.james.backends.es.AliasName;
import org.apache.james.backends.es.ClientProviderImpl;
import org.apache.james.backends.es.IndexCreationFactory;
import org.apache.james.backends.es.IndexName;
import org.apache.james.backends.es.IndexerSupplier;
import org.apache.james.backends.es.NodeMappingFactory;
import org.apache.james.backends.es.TypeName;
import org.apache.james.mailbox.elasticsearch.IndexAttachments;
import org.apache.james.mailbox.elasticsearch.MailboxElasticSearchConstants;
import org.apache.james.mailbox.elasticsearch.MailboxIndexerSupplier;
import org.apache.james.mailbox.elasticsearch.MailboxMappingFactory;
import org.apache.james.mailbox.elasticsearch.events.ElasticSearchListeningMessageSearchIndex;
import org.apache.james.mailbox.store.search.ListeningMessageSearchIndex;
import org.apache.james.mailbox.store.search.MessageSearchIndex;
import org.apache.james.utils.PropertiesProvider;
import org.apache.james.utils.RetryExecutorUtil;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.nurkiewicz.asyncretry.AsyncRetryExecutor;

public class ElasticSearchMailboxModule extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchMailboxModule.class);

    public static final String ELASTICSEARCH_CONFIGURATION_NAME = "elasticsearch";

    @Override
    protected void configure() {
        bind(ElasticSearchListeningMessageSearchIndex.class).in(Scopes.SINGLETON);
        bind(MessageSearchIndex.class).to(ElasticSearchListeningMessageSearchIndex.class);
        bind(ListeningMessageSearchIndex.class).to(ElasticSearchListeningMessageSearchIndex.class);
        bind(MailboxIndexerSupplier.class).in(Scopes.SINGLETON);

        bind(IndexerSupplier.class)
            .annotatedWith(Names.named(MailboxElasticSearchConstants.InjectionNames.MAILBOX_INDEX))
            .to(MailboxIndexerSupplier.class);
        bind(TypeName.class)
            .annotatedWith(Names.named(MailboxElasticSearchConstants.InjectionNames.MAILBOX_MAPPING))
            .toInstance(MailboxElasticSearchConstants.MESSAGE_TYPE);
    }

    @Provides
    @Singleton
    private ElasticSearchConfiguration getElasticSearchConfiguration(PropertiesProvider propertiesProvider) throws ConfigurationException {
        try {
            PropertiesConfiguration configuration = propertiesProvider.getConfiguration(ELASTICSEARCH_CONFIGURATION_NAME);
            return ElasticSearchConfiguration.fromProperties(configuration);
        } catch (FileNotFoundException e) {
            LOGGER.warn("Could not find " + ELASTICSEARCH_CONFIGURATION_NAME + " configuration file. Using 127.0.0.1:9300 as contact point");
            return ElasticSearchConfiguration.DEFAULT_CONFIGURATION;
        }
    }

    @Provides
    @Named(MailboxElasticSearchConstants.InjectionNames.MAILBOX_INDEX)
    protected IndexName provideIndexName(ElasticSearchConfiguration configuration) {
        return configuration.getIndexMailboxName();
    }

    @Provides
    @Named(MailboxElasticSearchConstants.InjectionNames.MAILBOX_READ_ALIAS)
    protected AliasName provideReadAliasName(ElasticSearchConfiguration configuration) {
        return configuration.getReadAliasMailboxName();
    }

    @Provides
    @Named(MailboxElasticSearchConstants.InjectionNames.MAILBOX_WRITE_ALIAS)
    protected AliasName provideWriteAliasName(ElasticSearchConfiguration configuration) {
        return configuration.getWriteAliasMailboxName();
    }

    @Provides
    @Singleton
    @Named(MailboxElasticSearchConstants.InjectionNames.MAILBOX)
    protected IndexCreationFactory provideIndexCreationFactory(ElasticSearchConfiguration configuration) {
        return new IndexCreationFactory()
            .useIndex(configuration.getIndexMailboxName())
            .addAlias(configuration.getReadAliasMailboxName())
            .addAlias(configuration.getWriteAliasMailboxName())
            .nbShards(configuration.getNbShards())
            .nbReplica(configuration.getNbReplica());
    }

    @Provides
    @Singleton
    protected Client provideClient(ElasticSearchConfiguration configuration,
                                   @Named(MailboxElasticSearchConstants.InjectionNames.MAILBOX) IndexCreationFactory mailboxIndexCreationFactory,
                                   AsyncRetryExecutor executor) throws ExecutionException, InterruptedException {

        return RetryExecutorUtil.retryOnExceptions(executor, configuration.getMaxRetries(), configuration.getMinDelay(), NoNodeAvailableException.class)
            .getWithRetry(context -> connectToCluster(configuration, ImmutableList.of(mailboxIndexCreationFactory)))
            .get();
    }

    private Client connectToCluster(ElasticSearchConfiguration configuration, Collection<IndexCreationFactory> indexCreationFactories) {
        LOGGER.info("Trying to connect to ElasticSearch service at {}", LocalDateTime.now());

        Client client = ClientProviderImpl.fromHosts(configuration.getHosts()).get();

        indexCreationFactories.forEach(factory -> factory.createIndexAndAliases(client));

        NodeMappingFactory.applyMapping(client,
            configuration.getIndexMailboxName(),
            MailboxElasticSearchConstants.MESSAGE_TYPE,
            MailboxMappingFactory.getMappingContent());

        return client;
    }

    @Provides
    @Singleton
    public IndexAttachments provideIndexAttachments(ElasticSearchConfiguration configuration) {
        return configuration.getIndexAttachment();
    }

}
