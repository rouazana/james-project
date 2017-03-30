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

import static org.apache.james.modules.server.ElasticSearchConfigurationKeys.ELASTICSEARCH_HOSTS;
import static org.apache.james.modules.server.ElasticSearchConfigurationKeys.ELASTICSEARCH_MASTER_HOST;
import static org.apache.james.modules.server.ElasticSearchConfigurationKeys.ELASTICSEARCH_PORT;
import static org.apache.james.modules.server.ElasticSearchConfigurationKeys.ES_CONFIG_FILE;

import java.io.FileNotFoundException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.inject.Singleton;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.james.backends.es.ClientProvider;
import org.apache.james.backends.es.ClientProviderImpl;
import org.apache.james.backends.es.IndexCreationFactory;
import org.apache.james.backends.es.IndexName;
import org.apache.james.backends.es.NodeMappingFactory;
import org.apache.james.backends.es.TypeName;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.mailbox.elasticsearch.IndexAttachments;
import org.apache.james.mailbox.elasticsearch.MailboxElasticsearchConstants;
import org.apache.james.mailbox.elasticsearch.MailboxMappingFactory;
import org.apache.james.mailbox.elasticsearch.events.ElasticSearchListeningMessageSearchIndex;
import org.apache.james.mailbox.extractor.TextExtractor;
import org.apache.james.mailbox.store.search.ListeningMessageSearchIndex;
import org.apache.james.mailbox.store.search.MessageSearchIndex;
import org.apache.james.mailbox.tika.extractor.TikaTextExtractor;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.nurkiewicz.asyncretry.AsyncRetryExecutor;

public class ElasticSearchMailboxModule extends AbstractModule {

    private static final int DEFAULT_CONNECTION_MAX_RETRIES = 7;
    private static final int DEFAULT_CONNECTION_MIN_DELAY = 3000;
    private static final boolean DEFAULT_INDEX_ATTACHMENTS = true;

    @Override
    protected void configure() {
        bind(IndexName.class).toInstance(MailboxElasticsearchConstants.MAILBOX_INDEX);
        bind(TypeName.class).toInstance(MailboxElasticsearchConstants.MESSAGE_TYPE);
        bind(ElasticSearchListeningMessageSearchIndex.class).in(Scopes.SINGLETON);
        bind(MessageSearchIndex.class).to(ElasticSearchListeningMessageSearchIndex.class);
        bind(ListeningMessageSearchIndex.class).to(ElasticSearchListeningMessageSearchIndex.class);

        bind(TikaTextExtractor.class).in(Scopes.SINGLETON);
        bind(TextExtractor.class).to(TikaTextExtractor.class);
    }

    @Provides
    @Singleton
    protected Client provideClientProvider(FileSystem fileSystem, AsyncRetryExecutor executor) throws ConfigurationException, FileNotFoundException, ExecutionException, InterruptedException {
        PropertiesConfiguration propertiesReader = new PropertiesConfiguration(fileSystem.getFile(ES_CONFIG_FILE));

        ClientProvider clientProvider = connectToCluster(propertiesReader);

        Client client = getRetryer(executor, propertiesReader)
                .getWithRetry(ctx -> clientProvider.get()).get();
        IndexCreationFactory.createIndex(client,
            MailboxElasticsearchConstants.MAILBOX_INDEX,
            propertiesReader.getInt("elasticsearch.nb.shards"),
            propertiesReader.getInt("elasticsearch.nb.replica"));
        NodeMappingFactory.applyMapping(client,
            MailboxElasticsearchConstants.MAILBOX_INDEX,
            MailboxElasticsearchConstants.MESSAGE_TYPE,
            MailboxMappingFactory.getMappingContent());
        return client;
    }

    private static ClientProvider connectToCluster(PropertiesConfiguration propertiesReader) throws ConfigurationException {
        Optional<String> monoHostAddress = Optional.ofNullable(propertiesReader.getString(ELASTICSEARCH_MASTER_HOST, null));
        Optional<Integer> monoHostPort = Optional.ofNullable(propertiesReader.getInteger(ELASTICSEARCH_PORT, null));
        Optional<String> multiHosts = Optional.ofNullable(propertiesReader.getString(ELASTICSEARCH_HOSTS, null));

        validateHostsConfigurationOptions(monoHostAddress, monoHostPort, multiHosts);

        if (monoHostAddress.isPresent()) {
            return ClientProviderImpl.forHost(monoHostAddress.get(), monoHostPort.get());
        } else {
            return ClientProviderImpl.fromHostsString(multiHosts.get());
        }
    }

    @VisibleForTesting
    static void validateHostsConfigurationOptions(Optional<String> monoHostAddress,
                                                          Optional<Integer> monoHostPort,
                                                          Optional<String> multiHosts) throws ConfigurationException {
        if (monoHostAddress.isPresent() != monoHostPort.isPresent()) {
            throw new ConfigurationException(ELASTICSEARCH_MASTER_HOST + " and " + ELASTICSEARCH_PORT + " should be specified together");
        }
        if (multiHosts.isPresent() && monoHostAddress.isPresent()) {
            throw new ConfigurationException("You should choose between mono host set up and " + ELASTICSEARCH_HOSTS);
        }
        if (!multiHosts.isPresent() && !monoHostAddress.isPresent()) {
            throw new ConfigurationException("You should specify either (" + ELASTICSEARCH_MASTER_HOST + " and " + ELASTICSEARCH_PORT + ") or " + ELASTICSEARCH_HOSTS);
        }
    }

    private static AsyncRetryExecutor getRetryer(AsyncRetryExecutor executor, PropertiesConfiguration configuration) {
        return executor
                .withProportionalJitter()
                .retryOn(NoNodeAvailableException.class)
                .withMaxRetries(configuration.getInt("elasticsearch.retryConnection.maxRetries", DEFAULT_CONNECTION_MAX_RETRIES))
                .withMinDelay(configuration.getInt("elasticsearch.retryConnection.minDelay", DEFAULT_CONNECTION_MIN_DELAY));
    }

    @Provides 
    @Singleton
    public IndexAttachments provideIndexAttachments(PropertiesConfiguration configuration) {
        if (configuration.getBoolean("elasticsearch.indexAttachments", DEFAULT_INDEX_ATTACHMENTS)) {
            return IndexAttachments.YES;
        }
        return IndexAttachments.NO;
    }

}
