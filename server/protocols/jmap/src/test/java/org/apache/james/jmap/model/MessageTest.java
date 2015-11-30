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
package org.apache.james.jmap.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;
import java.util.Optional;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class MessageTest {
    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenIdIsNull() {
        Message.builder().build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenIdIsEmpty() {
        Message.builder().id("").build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenBlobIdIsNull() {
        Message.builder().id("id").build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenBlobIdIsEmpty() {
        Message.builder().id("id").blobId("").build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenThreadIdIsNull() {
        Message.builder().id("id").blobId("blobId").build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenThreadIdIsEmpty() {
        Message.builder().id("id").blobId("blobId").threadId("").build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenMailboxIdsIsNull() {
        Message.builder().id("id").blobId("blobId").threadId("threadId").build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenHeadersIsNull() {
        Message.builder().id("id").blobId("blobId").threadId("threadId").mailboxIds(ImmutableList.of()).build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenSubjectIsNull() {
        Message.builder().id("id").blobId("blobId").threadId("threadId").mailboxIds(ImmutableList.of()).headers(ImmutableMap.of()).build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenSubjectIsEmpty() {
        Message.builder().id("id").blobId("blobId").threadId("threadId").mailboxIds(ImmutableList.of()).headers(ImmutableMap.of())
            .subject("").build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenSizeIsNull() {
        Message.builder().id("id").blobId("blobId").threadId("threadId").mailboxIds(ImmutableList.of()).headers(ImmutableMap.of())
            .subject("subject").build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenDateIsNull() {
        Message.builder().id("id").blobId("blobId").threadId("threadId").mailboxIds(ImmutableList.of()).headers(ImmutableMap.of())
            .subject("subject").size(123).build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenPreviewIsNull() {
        Message.builder().id("id").blobId("blobId").threadId("threadId").mailboxIds(ImmutableList.of()).headers(ImmutableMap.of())
            .subject("subject").size(123).date(ZonedDateTime.now()).build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenPreviewIsEmpty() {
        Message.builder().id("id").blobId("blobId").threadId("threadId").mailboxIds(ImmutableList.of()).headers(ImmutableMap.of())
            .subject("subject").size(123).date(ZonedDateTime.now()).preview("").build();
    }

    @Test
    public void buildShouldWorkWhenMandatoryFieldsArePresent() {
        ZonedDateTime currentDate = ZonedDateTime.now();
        Message expected = new Message("id", "blobId", "threadId", ImmutableList.of("mailboxId"), Optional.empty(), false, false, false, false, false, ImmutableMap.of("key", "value"), Optional.empty(),
                ImmutableList.of(), ImmutableList.of(), ImmutableList.of(), ImmutableList.of(), "subject", currentDate, 123, "preview", Optional.empty(), Optional.empty(), ImmutableList.of(), ImmutableMap.of());
        Message tested = Message.builder()
                .id("id")
                .blobId("blobId")
                .threadId("threadId")
                .mailboxIds(ImmutableList.of("mailboxId"))
                .headers(ImmutableMap.of("key", "value"))
                .subject("subject")
                .size(123)
                .date(currentDate)
                .preview("preview")
                .build();
        assertThat(tested).isEqualToComparingFieldByField(expected);
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenAttachedMessageIsNotMatchingAttachments() {
        Attachment simpleAttachment = Attachment.builder().blobId("blobId").type("type").name("name").size(123).build();
        ImmutableList<Attachment> attachments = ImmutableList.of(simpleAttachment);
        SubMessage simpleMessage = SubMessage.builder()
                .headers(ImmutableMap.of("key", "value"))
                .subject("subject")
                .date(ZonedDateTime.now())
                .build();
        ImmutableMap<String, SubMessage> attachedMessages = ImmutableMap.of("differentBlobId", simpleMessage);
        Message.builder()
            .id("id")
            .blobId("blobId")
            .threadId("threadId")
            .mailboxIds(ImmutableList.of("mailboxId"))
            .headers(ImmutableMap.of("key", "value"))
            .subject("subject")
            .size(123)
            .date(ZonedDateTime.now())
            .preview("preview")
            .attachments(attachments)
            .attachedMessages(attachedMessages)
            .build();
    }

    @Test
    public void buildShouldWorkWhenAllFieldsArePresent() {
        Emailer from = Emailer.builder().name("from").email("from@domain").build();
        ImmutableList<Emailer> to = ImmutableList.of(Emailer.builder().name("to").email("to@domain").build());
        ImmutableList<Emailer> cc = ImmutableList.of(Emailer.builder().name("cc").email("cc@domain").build());
        ImmutableList<Emailer> bcc = ImmutableList.of(Emailer.builder().name("bcc").email("bcc@domain").build());
        ImmutableList<Emailer> replyTo = ImmutableList.of(Emailer.builder().name("replyTo").email("replyTo@domain").build());
        ZonedDateTime currentDate = ZonedDateTime.now();
        Attachment simpleAttachment = Attachment.builder().blobId("blobId").type("type").name("name").size(123).build();
        ImmutableList<Attachment> attachments = ImmutableList.of(simpleAttachment);
        SubMessage simpleMessage = SubMessage.builder()
                .headers(ImmutableMap.of("key", "value"))
                .subject("subject")
                .date(currentDate)
                .build();
        ImmutableMap<String, SubMessage> attachedMessages = ImmutableMap.of("blobId", simpleMessage);
        Message expected = new Message(
                "id",
                "blobId",
                "threadId",
                ImmutableList.of("mailboxId"),
                Optional.of("inReplyToMessageId"), 
                true,
                true,
                true,
                true,
                true,
                ImmutableMap.of("key", "value"),
                Optional.of(from),
                to,
                cc,
                bcc,
                replyTo,
                "subject",
                currentDate,
                123,
                "preview",
                Optional.of("textBody"), 
                Optional.of("htmlBody"),
                attachments,
                attachedMessages);
        Message tested = Message.builder()
            .id("id")
            .blobId("blobId")
            .threadId("threadId")
            .mailboxIds(ImmutableList.of("mailboxId"))
            .inReplyToMessageId("inReplyToMessageId")
            .isUnread(true)
            .isFlagged(true)
            .isAnswered(true)
            .isDraft(true)
            .hasAttachment(true)
            .headers(ImmutableMap.of("key", "value"))
            .from(from)
            .to(to)
            .cc(cc)
            .bcc(bcc)
            .replyTo(replyTo)
            .subject("subject")
            .date(currentDate)
            .size(123)
            .preview("preview")
            .textBody("textBody")
            .htmlBody("htmlBody")
            .attachments(attachments)
            .attachedMessages(attachedMessages)
            .build();
        assertThat(tested).isEqualToComparingFieldByField(expected);
    }
}
