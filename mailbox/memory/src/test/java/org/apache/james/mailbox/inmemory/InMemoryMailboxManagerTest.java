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

package org.apache.james.mailbox.inmemory;

import org.apache.james.mailbox.acl.GroupMembershipResolver;
import org.apache.james.mailbox.acl.MailboxACLResolver;
import org.apache.james.mailbox.acl.SimpleGroupMembershipResolver;
import org.apache.james.mailbox.acl.UnionMailboxACLResolver;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.FakeAuthenticator;
import org.apache.james.mailbox.store.FakeAuthorizator;
import org.apache.james.mailbox.store.mail.model.impl.MessageParser;
import org.junit.runner.RunWith;
import org.xenei.junit.contract.Contract;
import org.xenei.junit.contract.ContractImpl;
import org.xenei.junit.contract.ContractSuite;
import org.xenei.junit.contract.IProducer;

import com.google.common.base.Throwables;

@RunWith(ContractSuite.class)
@ContractImpl(InMemoryMailboxManager.class)
public class InMemoryMailboxManagerTest {
    private static final int LIMIT_ANNOTATIONS = 3;
    private static final int LIMIT_ANNOTATION_SIZE = 30;

    private IProducer<InMemoryMailboxManager> producer = new IProducer<InMemoryMailboxManager>() {

        @Override
        public InMemoryMailboxManager newInstance() {
            MailboxACLResolver aclResolver = new UnionMailboxACLResolver();
            GroupMembershipResolver groupMembershipResolver = new SimpleGroupMembershipResolver();
            MessageParser messageParser = new MessageParser();

            InMemoryMailboxSessionMapperFactory mailboxSessionMapperFactory = new InMemoryMailboxSessionMapperFactory();
            MessageId.Factory messageIdFactory = new InMemoryMessageId.Factory();
            InMemoryMailboxManager mailboxManager = new InMemoryMailboxManager(mailboxSessionMapperFactory, new FakeAuthenticator(), FakeAuthorizator.defaultReject(),
                    aclResolver, groupMembershipResolver, messageParser, messageIdFactory, LIMIT_ANNOTATIONS, LIMIT_ANNOTATION_SIZE);

            try {
                mailboxManager.init();
            } catch (MailboxException e) {
                throw Throwables.propagate(e);
            }

            return mailboxManager;
        }

        @Override
        public void cleanUp() {
        }
    };

    @Contract.Inject
    public IProducer<InMemoryMailboxManager> getProducer() {
        return producer;
    }
    
}
