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

package org.apache.james.jmap.methods;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.SharedInputStream;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.james.jmap.exceptions.MailboxRoleNotFoundException;
import org.apache.james.jmap.methods.MessageWithId.CreationMessageEntry;
import org.apache.james.jmap.model.CreationMessage;
import org.apache.james.jmap.model.CreationMessageId;
import org.apache.james.jmap.model.Message;
import org.apache.james.jmap.model.MessageId;
import org.apache.james.jmap.model.MessageProperties;
import org.apache.james.jmap.model.MessageProperties.MessageProperty;
import org.apache.james.jmap.model.SetError;
import org.apache.james.jmap.model.SetMessagesRequest;
import org.apache.james.jmap.model.SetMessagesResponse;
import org.apache.james.jmap.model.mailbox.Role;
import org.apache.james.jmap.send.MailFactory;
import org.apache.james.jmap.send.MailMetadata;
import org.apache.james.jmap.send.MailSpool;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxMetaData;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MailboxQuery;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.mail.MailboxMapperFactory;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxId;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailboxMessage;
import org.apache.mailet.Mail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.functions.ThrowingFunction;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

public class SetMessagesCreationProcessor<Id extends MailboxId> implements SetMessagesProcessor<Id> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetMessagesCreationProcessor.class);

    private final MailboxMapperFactory<Id> mailboxMapperFactory;
    private final MailboxManager mailboxManager;
    private final MailboxSessionMapperFactory<Id> mailboxSessionMapperFactory;
    private final MIMEMessageConverter<Id> mimeMessageConverter;
    private final MailSpool mailSpool;
    private final MailFactory<Id> mailFactory;

    @Inject
    @VisibleForTesting
    SetMessagesCreationProcessor(MailboxMapperFactory<Id> mailboxMapperFactory,
                                 MailboxManager mailboxManager,
                                 MailboxSessionMapperFactory<Id> mailboxSessionMapperFactory,
                                 MIMEMessageConverter<Id> mimeMessageConverter,
                                 MailSpool mailSpool,
                                 MailFactory<Id> mailFactory) {
        this.mailboxMapperFactory = mailboxMapperFactory;
        this.mailboxManager = mailboxManager;
        this.mailboxSessionMapperFactory = mailboxSessionMapperFactory;
        this.mimeMessageConverter = mimeMessageConverter;
        this.mailSpool = mailSpool;
        this.mailFactory = mailFactory;
    }

    @Override
    public SetMessagesResponse process(SetMessagesRequest<Id> request, MailboxSession mailboxSession) {
        Mailbox<Id> outbox = getMailboxWithRole(mailboxSession, Role.OUTBOX).orElseThrow(() -> new MailboxRoleNotFoundException(Role.OUTBOX));
        Mailbox<Id> draft = getMailboxWithRole(mailboxSession, Role.DRAFTS).orElseThrow(() -> new MailboxRoleNotFoundException(Role.DRAFTS));

        // handle errors
        Predicate<CreationMessage<Id>> validMessagesTester = x -> x.isValid(outbox, draft);
        Predicate<CreationMessage<Id>> invalidMessagesTester = validMessagesTester.negate();
        SetMessagesResponse.Builder responseBuilder = SetMessagesResponse.builder()
                .notCreated(handleCreationErrors(invalidMessagesTester, request));

        return request.getCreate().entrySet().stream()
                .filter(e -> validMessagesTester.test(e.getValue()))
                .map(e -> new MessageWithId.CreationMessageEntry<Id>(e.getKey(), e.getValue()))
                .map(nuMsg -> dispatchCreation(nuMsg, mailboxSession, outbox, draft, buildMessageIdFunc(mailboxSession, outbox)))
//                .map(nuMsg -> createMessageInOutboxAndSend(nuMsg, mailboxSession, outbox, buildMessageIdFunc(mailboxSession, outbox)))
                .map(msg -> SetMessagesResponse.builder().created(ImmutableMap.of(msg.getCreationId(), msg.getMessage())).build())
                .reduce(responseBuilder, SetMessagesResponse.Builder::accumulator, SetMessagesResponse.Builder::combiner)
                .build();
    }

    private MessageWithId<Message> dispatchCreation(CreationMessageEntry<Id> nuMsg, MailboxSession mailboxSession, Mailbox<Id> outbox, Mailbox<Id> draft, Function<Long, MessageId> buildMessageIdFunc) {
//        nuMsg.getMessage().getMailboxIds().
        // TODO Auto-generated method stub
        return null;
    }

    private Map<CreationMessageId, SetError> handleCreationErrors(Predicate<CreationMessage<Id>> invalidMessagesTester,
                                                                  SetMessagesRequest<Id> request) {
        return request.getCreate().entrySet().stream()
                .filter(e -> invalidMessagesTester.test(e.getValue()))
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), buildSetErrorFromValidationResult(e.getValue().validate())))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    private SetError buildSetErrorFromValidationResult(List<ValidationResult> validationErrors) {
        String formattedValidationErrorMessage = validationErrors.stream()
                .map(err -> err.getProperty() + ": " + err.getErrorMessage())
                .collect(Collectors.joining("\\n"));
        Splitter propertiesSplitter = Splitter.on(',').trimResults().omitEmptyStrings();
        Set<MessageProperties.MessageProperty> properties = validationErrors.stream()
                .flatMap(err -> propertiesSplitter.splitToList(err.getProperty()).stream())
                .flatMap(MessageProperty::find)
                .collect(Collectors.toSet());
        return SetError.builder()
                .type("invalidProperties")
                .properties(properties)
                .description(formattedValidationErrorMessage)
                .build();
    }

    @VisibleForTesting
    protected MessageWithId<Message> createMessageInOutboxAndSend(MessageWithId.CreationMessageEntry<Id> createdEntry,
                                                           MailboxSession session,
                                                           Mailbox<Id> outbox, Function<Long, MessageId> buildMessageIdFromUid) {
        try {
            MessageMapper<Id> messageMapper = mailboxSessionMapperFactory.createMessageMapper(session);
            MailboxMessage<Id> newMailboxMessage = buildMailboxMessage(createdEntry, outbox);
            messageMapper.add(outbox, newMailboxMessage);
            Message jmapMessage = Message.fromMailboxMessage(newMailboxMessage, buildMessageIdFromUid);
            sendMessage(newMailboxMessage, jmapMessage, session);
            return new MessageWithId<>(createdEntry.getCreationId(), jmapMessage);
        } catch (MailboxException | MessagingException | IOException e) {
            throw Throwables.propagate(e);
        } catch (MailboxRoleNotFoundException e) {
            LOGGER.error("Could not find mailbox '%s' while trying to save message.", e.getRole().serialize());
            throw Throwables.propagate(e);
        }
    }

    private Function<Long, MessageId> buildMessageIdFunc(MailboxSession session, Mailbox<Id> outbox) {
        MailboxPath outboxPath = new MailboxPath(session.getPersonalSpace(), session.getUser().getUserName(), outbox.getName());
        return uid -> new MessageId(session.getUser(), outboxPath, uid);
    }

    private MailboxMessage<Id> buildMailboxMessage(MessageWithId.CreationMessageEntry<Id> createdEntry, Mailbox<Id> outbox) {
        byte[] messageContent = mimeMessageConverter.convert(createdEntry);
        SharedInputStream content = new SharedByteArrayInputStream(messageContent);
        long size = messageContent.length;
        int bodyStartOctet = 0;

        Flags flags = getMessageFlags(createdEntry.getMessage());
        PropertyBuilder propertyBuilder = buildPropertyBuilder();
        Id mailboxId = outbox.getMailboxId();
        Date internalDate = Date.from(createdEntry.getMessage().getDate().toInstant());

        return new SimpleMailboxMessage<>(internalDate, size,
                bodyStartOctet, content, flags, propertyBuilder, mailboxId);
    }

    @VisibleForTesting
    protected Optional<Mailbox<Id>> getMailboxWithRole(MailboxSession session, Role role) {
        try {
            return mailboxManager.search(MailboxQuery.builder(session)
                    .privateUserMailboxes().build(), session).stream()
                    .map(MailboxMetaData::getPath)
                    .filter(x -> hasRole(x, role))
                    .map(loadMailbox(session))
                    .findFirst();
        } catch (MailboxException | MailboxRoleNotFoundException e) {
            LOGGER.error("Unable to find a mailbox with role 'outbox'!");
            throw Throwables.propagate(e);
        }
    }

    private boolean hasRole(MailboxPath mailBoxPath, Role role) {
        return Role.from(mailBoxPath.getName())
                .map(role::equals)
                .orElse(false);
    }

    private ThrowingFunction<MailboxPath, Mailbox<Id>> loadMailbox(MailboxSession session) {
        return path -> mailboxMapperFactory.getMailboxMapper(session).findMailboxByPath(path);
    }

    private PropertyBuilder buildPropertyBuilder() {
        return new PropertyBuilder();
    }

    private Flags getMessageFlags(CreationMessage<Id> message) {
        Flags result = new Flags();
        if (!message.isIsUnread()) {
            result.add(Flags.Flag.SEEN);
        }
        if (message.isIsFlagged()) {
            result.add(Flags.Flag.FLAGGED);
        }
        if (message.isIsAnswered() || message.getInReplyToMessageId().isPresent()) {
            result.add(Flags.Flag.ANSWERED);
        }
        if (message.isIsDraft()) {
            result.add(Flags.Flag.DRAFT);
        }
        return result;
    }

    private void sendMessage(MailboxMessage<Id> mailboxMessage, Message jmapMessage, MailboxSession session) throws MessagingException, IOException {
        Mail mail = mailFactory.build(mailboxMessage, jmapMessage);
        MailMetadata metadata = new MailMetadata(jmapMessage.getId(), session.getUser().getUserName());
        mailSpool.send(mail, metadata);
    }
}
