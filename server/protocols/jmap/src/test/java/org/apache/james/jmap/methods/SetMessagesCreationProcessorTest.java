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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.Function;

import org.apache.james.jmap.exceptions.MailboxRoleNotFoundException;
import org.apache.james.jmap.model.CreationMessage;
import org.apache.james.jmap.model.CreationMessage.DraftEmailer;
import org.apache.james.jmap.model.CreationMessageId;
import org.apache.james.jmap.model.Message;
import org.apache.james.jmap.model.MessageId;
import org.apache.james.jmap.model.SetMessagesRequest;
import org.apache.james.jmap.model.SetMessagesResponse;
import org.apache.james.jmap.model.mailbox.Role;
import org.apache.james.jmap.send.MailFactory;
import org.apache.james.jmap.send.MailMetadata;
import org.apache.james.jmap.send.MailSpool;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.TestId;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.mailet.Mail;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class SetMessagesCreationProcessorTest {

    private static final Message FAKE_MESSAGE = Message.builder()
            .id(MessageId.of("user|outbox|1"))
            .blobId("anything")
            .threadId("anything")
            .mailboxIds(ImmutableList.of("mailboxId"))
            .headers(ImmutableMap.of())
            .subject("anything")
            .size(0)
            .date(ZonedDateTime.now())
            .preview("anything")
            .build();

    @Test
    public void processShouldReturnEmptyCreatedWhenRequestHasEmptyCreate() {
        SetMessagesCreationProcessor<TestId> sut = new SetMessagesCreationProcessor<TestId>(null, null, null, null, null, null) {
            @Override
            protected Optional<Mailbox<TestId>> getMailboxWithRole(MailboxSession session, Role role) {
                @SuppressWarnings("unchecked")
				Mailbox<TestId> fakeOutbox = (Mailbox<TestId>) mock(Mailbox.class);
                when(fakeOutbox.getName()).thenReturn("outbox");//TODO
                return Optional.of(fakeOutbox);
            }
        };
        SetMessagesRequest<TestId> requestWithEmptyCreate = SetMessagesRequest.<TestId>builder().build();

        SetMessagesResponse result = sut.process(requestWithEmptyCreate, buildStubbedSession());

        assertThat(result.getCreated()).isEmpty();
        assertThat(result.getNotCreated()).isEmpty();
    }

    private MailboxSession buildStubbedSession() {
        MailboxSession.User stubUser = mock(MailboxSession.User.class);
        when(stubUser.getUserName()).thenReturn("user");
        MailboxSession stubSession = mock(MailboxSession.class);
        when(stubSession.getPathDelimiter()).thenReturn('.');
        when(stubSession.getUser()).thenReturn(stubUser);
        when(stubSession.getPersonalSpace()).thenReturn("#private");
        return stubSession;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processShouldReturnNonEmptyCreatedWhenRequestHasNonEmptyCreateInOutbox() throws MailboxException {
        // Given
        MessageMapper<TestId> stubMapper = mock(MessageMapper.class);
        MailboxSessionMapperFactory<TestId> mockSessionMapperFactory = mock(MailboxSessionMapperFactory.class);
        when(mockSessionMapperFactory.createMessageMapper(any(MailboxSession.class)))
                .thenReturn(stubMapper);

        SetMessagesCreationProcessor<TestId> sut = new SetMessagesCreationProcessor<TestId>(null, null, mockSessionMapperFactory, null, null, null) {
            @Override
            protected MessageWithId<Message> createMessageInOutboxAndSend(MessageWithId.CreationMessageEntry<TestId> createdEntry, MailboxSession session, Mailbox<TestId> outbox, Function<Long, MessageId> buildMessageIdFromUid) {
                return new MessageWithId<>(createdEntry.getCreationId(), FAKE_MESSAGE);
            }
            @Override
            protected Optional<Mailbox<TestId>> getMailboxWithRole(MailboxSession session, Role role) {
                Mailbox<TestId> fakeOutbox = mock(Mailbox.class);
                when(fakeOutbox.getName()).thenReturn("outbox");//TODO
                return Optional.of(fakeOutbox);
            }
        };
        // When
        SetMessagesResponse result = sut.process(buildFakeCreationInOutboxRequest(), buildStubbedSession());

        // Then
        assertThat(result.getCreated()).isNotEmpty();
        assertThat(result.getNotCreated()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processShouldReturnNonEmptyCreatedWhenRequestHasNonEmptyCreateInDraft() throws MailboxException {
        // Given
        MessageMapper<TestId> stubMapper = mock(MessageMapper.class);
        MailboxSessionMapperFactory<TestId> mockSessionMapperFactory = mock(MailboxSessionMapperFactory.class);
        when(mockSessionMapperFactory.createMessageMapper(any(MailboxSession.class)))
                .thenReturn(stubMapper);

        SetMessagesCreationProcessor<TestId> sut = new SetMessagesCreationProcessor<TestId>(null, null, mockSessionMapperFactory, null, null, null) {
            @Override
            protected MessageWithId<Message> createMessageInOutboxAndSend(MessageWithId.CreationMessageEntry<TestId> createdEntry, MailboxSession session, Mailbox<TestId> outbox, Function<Long, MessageId> buildMessageIdFromUid) {
                return new MessageWithId<>(createdEntry.getCreationId(), FAKE_MESSAGE);
            }
            @Override
            protected Optional<Mailbox<TestId>> getMailboxWithRole(MailboxSession session, Role role) {
                Mailbox<TestId> fakeOutbox = mock(Mailbox.class);
                when(fakeOutbox.getName()).thenReturn("outbox");//TODO
                return Optional.of(fakeOutbox);
            }
        };
        // When
        SetMessagesResponse result = sut.process(buildFakeCreationInDraftRequest(), buildStubbedSession());

        // Then
        assertThat(result.getCreated()).isNotEmpty();
        assertThat(result.getNotCreated()).isEmpty();
    }

    @Test(expected = MailboxRoleNotFoundException.class)
    public void processShouldThrowWhenOutboxNotFound() {
        // Given
        SetMessagesCreationProcessor<TestId> sut = new SetMessagesCreationProcessor<TestId>(null, null, null, null, null, null) {
            @Override
            protected Optional<Mailbox<TestId>> getMailboxWithRole(MailboxSession session, Role role) {
                return Optional.empty();
            }
        };
        // When
        sut.process(buildFakeCreationInOutboxRequest(), null);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processShouldCallMessageMapperWhenRequestHasNonEmptyCreate() throws MailboxException {
        // Given
        Mailbox<TestId> fakeOutbox = mock(Mailbox.class);
        MessageMapper<TestId> mockMapper = mock(MessageMapper.class);
        MailboxSessionMapperFactory<TestId> stubSessionMapperFactory = mock(MailboxSessionMapperFactory.class);
        when(stubSessionMapperFactory.createMessageMapper(any(MailboxSession.class)))
                .thenReturn(mockMapper);
        MailSpool mockedMailSpool = mock(MailSpool.class);
        MailFactory<TestId> mockedMailFactory = mock(MailFactory.class);

        SetMessagesCreationProcessor<TestId> sut = new SetMessagesCreationProcessor<TestId>(null, null,
                stubSessionMapperFactory, new MIMEMessageConverter<TestId>(), mockedMailSpool, mockedMailFactory) {
            @Override
            protected Optional<Mailbox<TestId>> getMailboxWithRole(MailboxSession session, Role role) {
                TestId stubMailboxId = mock(TestId.class);
                when(stubMailboxId.serialize()).thenReturn("user|outbox|12345");
                when(fakeOutbox.getMailboxId()).thenReturn(stubMailboxId);
                when(fakeOutbox.getName()).thenReturn("outbox");
                return Optional.of(fakeOutbox);
            }
        };
        // When
        sut.process(buildFakeCreationInOutboxRequest(), buildStubbedSession());

        // Then
        verify(mockMapper).add(eq(fakeOutbox), any(MailboxMessage.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processShouldSendMailWhenRequestHasNonEmptyCreate() throws Exception {
        // Given
        Mailbox<TestId> fakeOutbox = mock(Mailbox.class);
        MessageMapper<TestId> mockMapper = mock(MessageMapper.class);
        MailboxSessionMapperFactory<TestId> stubSessionMapperFactory = mock(MailboxSessionMapperFactory.class);
        when(stubSessionMapperFactory.createMessageMapper(any(MailboxSession.class)))
                .thenReturn(mockMapper);
        MailSpool mockedMailSpool = mock(MailSpool.class);
        MailFactory<TestId> mockedMailFactory = mock(MailFactory.class);

        SetMessagesCreationProcessor<TestId> sut = new SetMessagesCreationProcessor<TestId>(null, null,
                stubSessionMapperFactory, new MIMEMessageConverter<TestId>(), mockedMailSpool, mockedMailFactory) {
            @Override
            protected Optional<Mailbox<TestId>> getMailboxWithRole(MailboxSession session, Role role) {
                TestId stubMailboxId = mock(TestId.class);
                when(stubMailboxId.serialize()).thenReturn("user|outbox|12345");
                when(fakeOutbox.getMailboxId()).thenReturn(stubMailboxId);
                when(fakeOutbox.getName()).thenReturn("outbox");//TODO
                return Optional.of(fakeOutbox);
            }
        };
        // When
        sut.process(buildFakeCreationInOutboxRequest(), buildStubbedSession());

        // Then
        verify(mockedMailSpool).send(any(Mail.class), any(MailMetadata.class));
    }

    private SetMessagesRequest<TestId> buildFakeCreationInOutboxRequest() {
        return SetMessagesRequest.<TestId>builder()
                .create(ImmutableMap.of(CreationMessageId.of("anything-really"), CreationMessage.<TestId>builder()
                    .from(DraftEmailer.builder().name("alice").email("alice@example.com").build())
                    .to(ImmutableList.of(DraftEmailer.builder().name("bob").email("bob@example.com").build()))
                    .subject("Hey! ")
                    .mailboxIds(ImmutableList.of("outbox-id"))
                    .build()
                ))
                .build();
    }

    private SetMessagesRequest<TestId> buildFakeCreationInDraftRequest() {
        return SetMessagesRequest.<TestId>builder()
                .create(ImmutableMap.of(CreationMessageId.of("anything-really"), CreationMessage.<TestId>builder()
                    .from(DraftEmailer.builder().name("alice").email("alice@example.com").build())
                    .to(ImmutableList.of(DraftEmailer.builder().name("bob").email("bob@example.com").build()))
                    .subject("Hey! ")
                    .mailboxIds(ImmutableList.of("draft-id"))
                    .build()
                ))
                .build();
    }
}