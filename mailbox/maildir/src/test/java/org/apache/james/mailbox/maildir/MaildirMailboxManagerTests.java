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
package org.apache.james.mailbox.maildir;

import java.io.IOException;

import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxManagerTest;
import org.apache.james.mailbox.acl.GroupMembershipResolver;
import org.apache.james.mailbox.acl.MailboxACLResolver;
import org.apache.james.mailbox.acl.SimpleGroupMembershipResolver;
import org.apache.james.mailbox.acl.UnionMailboxACLResolver;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.store.Authenticator;
import org.apache.james.mailbox.store.Authorizator;
import org.apache.james.mailbox.store.JVMMailboxPathLocker;
import org.apache.james.mailbox.store.StoreMailboxManager;
import org.apache.james.mailbox.store.mail.model.DefaultMessageId;
import org.apache.james.mailbox.store.mail.model.impl.MessageParser;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;
import org.xenei.junit.contract.Contract;
import org.xenei.junit.contract.ContractImpl;
import org.xenei.junit.contract.ContractSuite;
import org.xenei.junit.contract.IProducer;

import com.google.common.base.Throwables;

@SuiteClasses({
    MaildirMailboxManagerTests.DomainUser.class,
    MaildirMailboxManagerTests.User.class,
    MaildirMailboxManagerTests.FullUser.class})

public class MaildirMailboxManagerTests {

    public static abstract class MaildirMailboxManagerTest<T extends MailboxManager> extends MailboxManagerTest<T> {
        protected StoreMailboxManager createMailboxManager(String configuration, TemporaryFolder temporaryFolder) throws MailboxException, IOException {
            MaildirStore store = new MaildirStore(temporaryFolder.newFolder().getPath() + configuration, new JVMMailboxPathLocker());
            MaildirMailboxSessionMapperFactory mf = new MaildirMailboxSessionMapperFactory(store);
            
            MailboxACLResolver aclResolver = new UnionMailboxACLResolver();
            GroupMembershipResolver groupMembershipResolver = new SimpleGroupMembershipResolver();
            MessageParser messageParser = new MessageParser();

            Authenticator noAuthenticator = null;
            Authorizator noAuthorizator = null;
            StoreMailboxManager manager = new StoreMailboxManager(mf, noAuthenticator, noAuthorizator, new JVMMailboxPathLocker(), aclResolver, 
                    groupMembershipResolver, messageParser, new DefaultMessageId.Factory());
            manager.init();

            return manager;
        }

    }

    @RunWith(ContractSuite.class)
    @ContractImpl(StoreMailboxManager.class)
    public static class DomainUser extends MaildirMailboxManagerTest<StoreMailboxManager> {
        @Rule public TemporaryFolder tmpFolder = new TemporaryFolder();

        private IProducer<StoreMailboxManager> producer = new IProducer<StoreMailboxManager>() {

            @Override
            public StoreMailboxManager newInstance() {
                try {
                    tmpFolder.create();
                    return createMailboxManager("/%domain/%user", tmpFolder);
                } catch (Exception e) {
                    throw Throwables.propagate(e);
                }
            }

            @Override
            public void cleanUp() {
                tmpFolder.delete();
            }
        };

        @Contract.Inject
        public IProducer<StoreMailboxManager> getProducer() {
            return producer;
        }
    }
    
    @Ignore
    @RunWith(ContractSuite.class)
    @ContractImpl(StoreMailboxManager.class)
    public static class User extends MaildirMailboxManagerTest<StoreMailboxManager> {
        @Rule public TemporaryFolder tmpFolder = new TemporaryFolder();

        private IProducer<StoreMailboxManager> producer = new IProducer<StoreMailboxManager>() {

            @Override
            public StoreMailboxManager newInstance() {
                try {
                    tmpFolder.create();
                    return createMailboxManager("/%user", tmpFolder);
                } catch (Exception e) {
                    throw Throwables.propagate(e);
                }
            }

            @Override
            public void cleanUp() {
                tmpFolder.delete();
            }
        };

        @Contract.Inject
        public IProducer<StoreMailboxManager> getProducer() {
            return producer;
        }
    }

    @RunWith(ContractSuite.class)
    @ContractImpl(StoreMailboxManager.class)
    public static class FullUser extends MaildirMailboxManagerTest<StoreMailboxManager> {
        @Rule public TemporaryFolder tmpFolder = new TemporaryFolder();

        private IProducer<StoreMailboxManager> producer = new IProducer<StoreMailboxManager>() {

            @Override
            public StoreMailboxManager newInstance() {
                try {
                    tmpFolder.create();
                    return createMailboxManager("/%fulluser", tmpFolder);
                } catch (Exception e) {
                    throw Throwables.propagate(e);
                }
            }

            @Override
            public void cleanUp() {
                tmpFolder.delete();
            }
        };

        @Contract.Inject
        public IProducer<StoreMailboxManager> getProducer() {
            return producer;
        }
    }

}
