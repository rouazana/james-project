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
package org.apache.james.mpt.smtp;

import java.io.IOException;

import javax.inject.Named;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.init.CassandraModuleComposite;
import org.apache.james.core.filesystem.FileSystemImpl;
import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.domainlist.api.DomainList;
import org.apache.james.domainlist.cassandra.CassandraDomainListModule;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.filesystem.api.JamesDirectoriesProvider;
import org.apache.james.mailbox.cassandra.modules.CassandraMailboxModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMessageModule;
import org.apache.james.modules.data.CassandraRecipientRewriteTableModule;
import org.apache.james.modules.protocols.ManageSieveServerModule;
import org.apache.james.modules.protocols.ProtocolHandlerModule;
import org.apache.james.modules.protocols.SMTPServerModule;
import org.apache.james.modules.server.ActiveMQQueueModule;
import org.apache.james.modules.server.CamelMailetContainerModule;
import org.apache.james.modules.server.ConfigurationProviderModule;
import org.apache.james.modules.server.DNSServiceModule;
import org.apache.james.modules.server.MailStoreRepositoryModule;
import org.apache.james.modules.server.SieveModule;
import org.apache.james.mpt.api.SmtpHostSystem;
import org.apache.james.mpt.smtp.dns.InMemoryDNSService;
import org.apache.james.mpt.smtp.host.JamesSmtpHostSystem;
import org.apache.james.rrt.api.RecipientRewriteTable;
import org.apache.james.rrt.cassandra.CassandraRRTModule;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.cassandra.CassandraUsersRepositoryModule;
import org.apache.james.utils.ConfigurationProvider;
import org.apache.james.utils.ConfigurationsPerformer;
import org.apache.james.utils.FileConfigurationProvider;
import org.apache.onami.lifecycle.jsr250.PreDestroyModule;
import org.junit.rules.TemporaryFolder;

import com.datastax.driver.core.Session;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.util.Modules;

public class SmtpTestModule extends AbstractModule {

    private final TemporaryFolder folder = new TemporaryFolder();
    private final String rootDirectory;
    private final CassandraCluster cassandraClusterSingleton;

    public SmtpTestModule() throws IOException {
        folder.create();
        rootDirectory = folder.newFolder().getAbsolutePath();
        CassandraModuleComposite cassandraModuleComposite = new CassandraModuleComposite(
                new CassandraMailboxModule(),
                new CassandraMessageModule(),
                new CassandraDomainListModule(),
                new CassandraUsersRepositoryModule(),
                new CassandraRRTModule());
        cassandraClusterSingleton = CassandraCluster.create(cassandraModuleComposite);
    }

    @Override
    protected void configure() {
        install(Modules.override(Modules.combine(
                new org.apache.james.modules.data.CassandraUsersRepositoryModule(),
                new org.apache.james.modules.data.CassandraDomainListModule(),
                new CassandraRecipientRewriteTableModule(),
                new DNSServiceModule(),
                new ProtocolHandlerModule(),
                new SMTPServerModule(),
                new ManageSieveServerModule(),
                new ActiveMQQueueModule(),
                new SieveModule(),
                new MailStoreRepositoryModule(),
                new CamelMailetContainerModule(),
                new ConfigurationProviderModule(),
                new PreDestroyModule()))
            .with(Modules.combine(
                new FileSystemModule(rootDirectory),
                new CassandraModule(cassandraClusterSingleton.getConf()),
                new DNSModule())));
    }

    private static class FileSystemModule extends AbstractModule {

        private static final String CONFIGURATION_PATH = "configurationPath";

        private final String rootDirectory;

        public FileSystemModule(String rootDirectory) {
            this.rootDirectory = rootDirectory;
        }

        @Provides @Singleton @Named(CONFIGURATION_PATH)
        public String configurationPath() {
            return FileSystem.FILE_PROTOCOL_AND_CONF;
        }

        @Override
        protected void configure() {
            bind(FileSystem.class).to(FileSystemImpl.class);
            bind(ConfigurationProvider.class).to(FileConfigurationProvider.class);
            bind(JamesDirectoriesProvider.class).toInstance(new MyJamesDirectoriesProvider(rootDirectory));
        }

        private static class MyJamesDirectoriesProvider implements JamesDirectoriesProvider {

            private final String rootDirectory;

            public MyJamesDirectoriesProvider(String rootDirectory) {
                this.rootDirectory = rootDirectory;
            }

            @Override
            public String getAbsoluteDirectory() {
                return "/";
            }

            @Override
            public String getConfDirectory() {
                return ClassLoader.getSystemResource("conf").getPath();
            }

            @Override
            public String getVarDirectory() {
                return rootDirectory + "/var/";
            }

            @Override
            public String getRootDirectory() {
                return rootDirectory;
            }
        }
    }

    private static class CassandraModule extends AbstractModule {

        private final Session session;

        public CassandraModule(Session session) {
            this.session = session;
        }

        @Override
        protected void configure() {
        }

        @Provides
        public com.datastax.driver.core.Session cassandraSession() {
            return session;
        }
    }

    private static class DNSModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(InMemoryDNSService.class).in(Scopes.SINGLETON);
            bind(DNSService.class).to(InMemoryDNSService.class);
        }
    }

    @Provides
    @Singleton
    public SmtpHostSystem provideHostSystem(ConfigurationsPerformer configurationsPerformer, DomainList domainList, UsersRepository usersRepository, RecipientRewriteTable recipientRewriteTable) throws Exception {
        return new JamesSmtpHostSystem(configurationsPerformer, domainList, usersRepository, recipientRewriteTable);
    }
}
