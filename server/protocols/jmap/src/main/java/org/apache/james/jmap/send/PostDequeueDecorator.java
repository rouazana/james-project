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
package org.apache.james.jmap.send;

import java.util.Iterator;

import org.apache.james.jmap.model.MessageId;
import org.apache.james.jmap.model.mailbox.Role;
import org.apache.james.jmap.send.exception.MailShouldBeInOutboxException;
import org.apache.james.jmap.send.exception.MailboxRoleNotFoundException;
import org.apache.james.jmap.send.exception.MessageIdNotFoundException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxMetaData;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MailboxQuery;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.store.mail.MailboxMapperFactory;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.MessageMapperFactory;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxId;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.queue.api.MailQueue.MailQueueException;
import org.apache.james.queue.api.MailQueue.MailQueueItem;
import org.apache.james.queue.api.MailQueueItemDecoratorFactory.MailQueueItemDecorator;
import org.apache.mailet.Mail;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostDequeueDecorator<Id extends MailboxId> extends MailQueueItemDecorator {
    private static final Logger LOG = LoggerFactory.getLogger(PostDequeueDecorator.class);

    private final MailboxManager mailboxManager;
    private final MessageMapperFactory<Id> messageMapperFactory;
    private final MailboxMapperFactory<Id> mailboxMapperFactory;

    public PostDequeueDecorator(MailQueueItem mailQueueItem,
            MailboxManager mailboxManager,
            MessageMapperFactory<Id> messageMapperFactory,
            MailboxMapperFactory<Id> mailboxMapperFactory) {
        super(mailQueueItem);
        this.mailboxManager = mailboxManager;
        this.messageMapperFactory = messageMapperFactory;
        this.mailboxMapperFactory = mailboxMapperFactory;
    }

    @Override
    public Mail getMail() {
        return mailQueueItem.getMail();
    }

    @Override
    public void done(boolean success) throws MailQueueException {
        mailQueueItem.done(success);
        if (success) {
            MessageId messageId = MessageId.of(getMail().getName());
            MailboxSession mailboxSession = createSession(messageId);
            Pair<MailboxMessage<Id>, MailboxPath> mailboxMessageAndMailboxPath = getMailboxMessageAndMailboxPath(messageId, mailboxSession);
            moveFromOutboxToSent(mailboxMessageAndMailboxPath, mailboxSession);
        }
    }

    private MailboxSession createSession(MessageId messageId) throws MailQueueException {
        MailboxSession mailboxSession;
        try {
            mailboxSession = mailboxManager.createSystemSession(messageId.getUsername(), LOG);
        } catch (MailboxException e) {
            throw new MailQueueException(e.getMessage(), e);
        }
        return mailboxSession;
    }
    
    public Pair<MailboxMessage<Id>, MailboxPath> getMailboxMessageAndMailboxPath(MessageId messageId, MailboxSession mailboxSession) throws MailQueueException {
        try {
            MailboxPath mailboxPath = messageId.getMailboxPath(mailboxSession);
            MessageMapper<Id> messageMapper = messageMapperFactory.getMessageMapper(mailboxSession);
            Mailbox<Id> mailbox = mailboxMapperFactory.getMailboxMapper(mailboxSession).findMailboxByPath(mailboxPath);
            Iterator<MailboxMessage<Id>> resultIterator = messageMapper.findInMailbox(mailbox, MessageRange.one(messageId.getUid()), MessageMapper.FetchType.Full, 1);
            if (resultIterator.hasNext()) {
                return Pair.with(resultIterator.next(), mailboxPath);
            } else {
                throw new MessageIdNotFoundException(messageId);
            }
        } catch (MailboxException e) {
            throw new MailQueueException(e.getMessage(), e);
        }
    }

    private void moveFromOutboxToSent(Pair<MailboxMessage<Id>, MailboxPath> mailboxMessageAndMailboxPath, MailboxSession mailboxSession) throws MailQueueException {
        MailboxMessage<Id> mailboxMessage = mailboxMessageAndMailboxPath.getValue0();
        MailboxPath outboxMailboxPath = mailboxMessageAndMailboxPath.getValue1();
        ensureMailboxPathIsOutbox(outboxMailboxPath);
        try {
            MailboxPath sentMailboxPath = getSentMailboxPath(mailboxSession);
            // MOVE is not implemented, so COPY and DELETE
            mailboxManager.copyMessages(MessageRange.one(mailboxMessage.getUid()), outboxMailboxPath, sentMailboxPath, mailboxSession);
            Mailbox<Id> outboxMailbox = mailboxMapperFactory.getMailboxMapper(mailboxSession).findMailboxByPath(outboxMailboxPath);
            messageMapperFactory.getMessageMapper(mailboxSession).delete(outboxMailbox, mailboxMessage);
        } catch (MailboxException e) {
            throw new MailQueueException(e.getMessage(), e);
        }
    }

    private void ensureMailboxPathIsOutbox(MailboxPath outboxMailboxPath) throws MailShouldBeInOutboxException {
        if (!hasRole(outboxMailboxPath, Role.OUTBOX)) {
            throw new MailShouldBeInOutboxException(outboxMailboxPath);
        }
    }

    private MailboxPath getSentMailboxPath(MailboxSession session) throws MailboxRoleNotFoundException, MailboxException {
        MailboxQuery allUserMailboxesQuery = MailboxQuery.builder(session)
            .privateUserMailboxes()
            .build();
        return mailboxManager.search(allUserMailboxesQuery, session)
                .stream()
                .map(MailboxMetaData::getPath)
                .filter(path -> hasRole(path, Role.SENT))
                .findFirst()
                .orElseThrow(() -> new MailboxRoleNotFoundException(Role.SENT));
    }
    
    private boolean hasRole(MailboxPath mailBoxPath, Role role) {
        return Role.from(mailBoxPath.getName())
                .map(role::equals)
                .orElse(false);
    }
}
