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

import javax.inject.Singleton;

import org.apache.james.adapter.mailbox.store.UserRepositoryAuthenticator;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxPathLocker;
import org.apache.james.mailbox.SubscriptionManager;
import org.apache.james.mailbox.acl.GroupMembershipResolver;
import org.apache.james.mailbox.acl.MailboxACLResolver;
import org.apache.james.mailbox.acl.SimpleGroupMembershipResolver;
import org.apache.james.mailbox.acl.UnionMailboxACLResolver;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.inmemory.InMemoryId;
import org.apache.james.mailbox.inmemory.InMemoryMailboxManager;
import org.apache.james.mailbox.inmemory.InMemoryMailboxSessionMapperFactory;
import org.apache.james.mailbox.inmemory.mail.InMemoryModSeqProvider;
import org.apache.james.mailbox.inmemory.mail.InMemoryUidProvider;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.store.Authenticator;
import org.apache.james.mailbox.store.JVMMailboxPathLocker;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.StoreSubscriptionManager;
import org.apache.james.mailbox.store.extractor.TextExtractor;
import org.apache.james.mailbox.store.mail.AttachmentMapperFactory;
import org.apache.james.mailbox.store.mail.MailboxMapperFactory;
import org.apache.james.mailbox.store.mail.MessageMapperFactory;
import org.apache.james.mailbox.store.mail.ModSeqProvider;
import org.apache.james.mailbox.store.mail.UidProvider;
import org.apache.james.mailbox.store.search.MessageSearchIndex;
import org.apache.james.mailbox.store.search.SimpleMessageSearchIndex;
import org.apache.james.mailbox.store.user.SubscriptionMapperFactory;
import org.apache.james.mailbox.tika.extractor.TikaTextExtractor;
import org.apache.james.modules.Names;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Named;

public class MemoryMailboxModule extends AbstractModule {

    @Override
    protected void configure() {

        bind(MessageMapperFactory.class).to(InMemoryMailboxSessionMapperFactory.class);
        bind(MailboxMapperFactory.class).to(InMemoryMailboxSessionMapperFactory.class);
        bind(AttachmentMapperFactory.class).to(InMemoryMailboxSessionMapperFactory.class);
        bind(MailboxSessionMapperFactory.class).to(InMemoryMailboxSessionMapperFactory.class);
        bind(ModSeqProvider.class).to(InMemoryModSeqProvider.class);
        bind(UidProvider.class).to(InMemoryUidProvider.class);
        bind(MailboxId.Factory.class).to(InMemoryId.Factory.class);

        bind(SubscriptionManager.class).to(StoreSubscriptionManager.class);
        bind(SubscriptionMapperFactory.class).to(InMemoryMailboxSessionMapperFactory.class);
        bind(MailboxSessionMapperFactory.class).to(InMemoryMailboxSessionMapperFactory.class);
        bind(MailboxPathLocker.class).to(JVMMailboxPathLocker.class);
        bind(Authenticator.class).to(UserRepositoryAuthenticator.class);
        bind(MailboxManager.class).to(InMemoryMailboxManager.class);
        bind(MailboxACLResolver.class).to(UnionMailboxACLResolver.class);
        bind(GroupMembershipResolver.class).to(SimpleGroupMembershipResolver.class);

        bind(MessageSearchIndex.class).to(SimpleMessageSearchIndex.class);
        bind(TextExtractor.class).to(TikaTextExtractor.class);

        bind(InMemoryMailboxSessionMapperFactory.class).in(Scopes.SINGLETON);
        bind(InMemoryModSeqProvider.class).in(Scopes.SINGLETON);
        bind(InMemoryUidProvider.class).in(Scopes.SINGLETON);
        bind(StoreSubscriptionManager.class).in(Scopes.SINGLETON);
        bind(JVMMailboxPathLocker.class).in(Scopes.SINGLETON);
        bind(UserRepositoryAuthenticator.class).in(Scopes.SINGLETON);
        bind(InMemoryMailboxManager.class).in(Scopes.SINGLETON);
        bind(UnionMailboxACLResolver.class).in(Scopes.SINGLETON);
        bind(SimpleGroupMembershipResolver.class).in(Scopes.SINGLETON);
    }

    @Provides @Named(Names.MAILBOXMANAGER_NAME) @Singleton
    public MailboxManager provideMailboxManager(InMemoryMailboxManager mailboxManager) throws MailboxException {
        mailboxManager.init();
        return mailboxManager;
    }
}