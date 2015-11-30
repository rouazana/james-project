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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.james.mailbox.store.TestId;
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMessage;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class MessageTest {
    private static final TestId MAILBOX_ID = TestId.of(18L);
    private static final long MOD_SEQ = 42L;
    private static final ZoneId UTC_ZONE_ID = ZoneId.of("Z");
    private static final ZonedDateTime ZONED_DATE = ZonedDateTime.of(2015, 07, 14, 12, 30, 42, 0, UTC_ZONE_ID);
    private static final Date INTERNAL_DATE = Date.from(ZONED_DATE.toInstant());

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

    @Test
    public void emptyMailShouldBeLoadedIntoMessage() throws Exception {
        org.apache.james.mailbox.store.mail.model.Message<TestId> testMail = new SimpleMessage<>(
                INTERNAL_DATE,
                0,
                0,
                new SharedByteArrayInputStream("".getBytes()),
                new Flags(Flag.SEEN),
                new PropertyBuilder(),
                MAILBOX_ID);
        testMail.setModSeq(MOD_SEQ);
        
        Message testee = Message.fromMailboxMessage(testMail);
        Message expected = Message.builder()
                .id("0")
                .blobId("0")
                .threadId("0")
                .mailboxIds(ImmutableList.of(MAILBOX_ID.serialize()))
                .headers(ImmutableMap.of())
                .subject("(No subject)")
                .size(0)
                .date(ZONED_DATE)
                .preview("(Empty)")
                .build();
        assertThat(testee).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void flagsShouldBeSetIntoMessage() throws Exception {
        Flags flags = new Flags();
        flags.add(Flag.ANSWERED);
        flags.add(Flag.FLAGGED);
        flags.add(Flag.DRAFT);
        org.apache.james.mailbox.store.mail.model.Message<TestId> testMail = new SimpleMessage<>(
                INTERNAL_DATE,
                0,
                0,
                new SharedByteArrayInputStream("".getBytes()),
                flags,
                new PropertyBuilder(),
                MAILBOX_ID);
        testMail.setModSeq(MOD_SEQ);
        
        Message testee = Message.fromMailboxMessage(testMail);
        Message expected = Message.builder()
                .id("0")
                .blobId("0")
                .threadId("0")
                .mailboxIds(ImmutableList.of(MAILBOX_ID.serialize()))
                .isUnread(true)
                .isFlagged(true)
                .isAnswered(true)
                .isDraft(true)
                .headers(ImmutableMap.of())
                .subject("(No subject)")
                .size(0)
                .date(ZONED_DATE)
                .preview("(Empty)")
                .build();
        assertThat(testee).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void headersShouldBeSetIntoMessage() throws Exception {
        String headers = "From: user <user@domain>\n"
                + "Subject: test subject\n"
                + "To: user1 <user1@domain>, user2 <user2@domain>\n"
                + "Cc: usercc <usercc@domain>\n"
                + "Bcc: userbcc <userbcc@domain>\n"
                + "Reply-To: \"user to reply to\" <user.reply.to@domain>\n"
                + "In-Reply-To: <SNT124-W2664003139C1E520CF4F6787D30@phx.gbl>\n"
                + "Other-header: other header value";
        org.apache.james.mailbox.store.mail.model.Message<TestId> testMail = new SimpleMessage<>(
                INTERNAL_DATE,
                headers.length(),
                headers.length(),
                new SharedByteArrayInputStream(headers.getBytes()),
                new Flags(Flag.SEEN),
                new PropertyBuilder(),
                MAILBOX_ID);
        testMail.setModSeq(MOD_SEQ);

        Emailer user = Emailer.builder().name("user").email("user@domain").build();
        Emailer user1 = Emailer.builder().name("user1").email("user1@domain").build();
        Emailer user2 = Emailer.builder().name("user2").email("user2@domain").build();
        Emailer usercc = Emailer.builder().name("usercc").email("usercc@domain").build();
        Emailer userbcc = Emailer.builder().name("userbcc").email("userbcc@domain").build();
        Emailer userRT = Emailer.builder().name("user to reply to").email("user.reply.to@domain").build();
        ImmutableMap<String, String> headersMap = ImmutableMap.<String, String>builder()
                .put("cc", "usercc <usercc@domain>")
                .put("bcc", "userbcc <userbcc@domain>")
                .put("subject", "test subject")
                .put("from", "user <user@domain>")
                .put("to", "user1 <user1@domain>, user2 <user2@domain>")
                .put("reply-to", "\"user to reply to\" <user.reply.to@domain>")
                .put("in-reply-to", "<SNT124-W2664003139C1E520CF4F6787D30@phx.gbl>")
                .put("other-header", "other header value")
                .build();
        Message testee = Message.fromMailboxMessage(testMail);
        Message expected = Message.builder()
                .id("0")
                .blobId("0")
                .threadId("0")
                .mailboxIds(ImmutableList.of(MAILBOX_ID.serialize()))
                .inReplyToMessageId("<SNT124-W2664003139C1E520CF4F6787D30@phx.gbl>")
                .headers(headersMap)
                .from(user)
                .to(ImmutableList.of(user1, user2))
                .cc(ImmutableList.of(usercc))
                .bcc(ImmutableList.of(userbcc))
                .replyTo(ImmutableList.of(userRT))
                .subject("test subject")
                .date(ZONED_DATE)
                .size(headers.length())
                .preview("(Empty)")
                .build();
        assertThat(testee).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void textBodyShouldBeSetIntoMessage() throws Exception {
        String headers = "Subject: test subject\n";
        String body = "Mail body";
        String mail = headers + "\n" + body;
        org.apache.james.mailbox.store.mail.model.Message<TestId> testMail = new SimpleMessage<>(
                INTERNAL_DATE,
                mail.length(),
                headers.length(),
                new SharedByteArrayInputStream(mail.getBytes()),
                new Flags(Flag.SEEN),
                new PropertyBuilder(),
                MAILBOX_ID);
        testMail.setModSeq(MOD_SEQ);
        
        Message testee = Message.fromMailboxMessage(testMail);
        Message expected = Message.builder()
                .id("0")
                .blobId("0")
                .threadId("0")
                .mailboxIds(ImmutableList.of(MAILBOX_ID.serialize()))
                .headers(ImmutableMap.of("subject", "test subject"))
                .subject("test subject")
                .size(mail.length())
                .date(ZONED_DATE)
                .preview("Mail body")
                .textBody("Mail body")
                .build();
        assertThat(testee).isEqualToComparingFieldByField(expected);
    }
    
    @Test
    public void bodyWith256LengthShouldNotBeTruncated() {
        String body256 = "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999"
                + "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999"
                + "00000000001111111111222222222233333333334444444444555555";
        assertThat(body256.length()).isEqualTo(256);
        assertThat(Message.computePreview(body256)).isEqualTo(body256);
    }
    
    @Test
    public void bodyWith257LengthShouldBeTruncated() {
        String body257 = "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999"
                + "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999"
                + "000000000011111111112222222222333333333344444444445555555";
        String expected = "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999"
                + "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999"
                + "00000000001111111111222222222233333333334444444444555...";
        assertThat(body257.length()).isEqualTo(257);
        assertThat(expected.length()).isEqualTo(256);
        assertThat(Message.computePreview(body257)).isEqualTo(expected);
    }

    @Test
    public void previewShouldBeLimitedTo256Length() throws Exception {
        String headers = "Subject: test subject\n";
        String body300 = "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999"
                + "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999"
                + "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999";
        String expectedPreview = "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999" 
                + "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999" 
                + "00000000001111111111222222222233333333334444444444555...";
        assertThat(body300.length()).isEqualTo(300);
        assertThat(expectedPreview.length()).isEqualTo(256);
        String mail = headers + "\n" + body300;
        org.apache.james.mailbox.store.mail.model.Message<TestId> testMail = new SimpleMessage<>(
                INTERNAL_DATE,
                mail.length(),
                headers.length(),
                new SharedByteArrayInputStream(mail.getBytes()),
                new Flags(Flag.SEEN),
                new PropertyBuilder(),
                MAILBOX_ID);
        testMail.setModSeq(MOD_SEQ);
        
        Message testee = Message.fromMailboxMessage(testMail);
        Message expected = Message.builder()
                .id("0")
                .blobId("0")
                .threadId("0")
                .mailboxIds(ImmutableList.of(MAILBOX_ID.serialize()))
                .headers(ImmutableMap.of("subject", "test subject"))
                .subject("test subject")
                .size(mail.length())
                .date(ZONED_DATE)
                .preview(expectedPreview)
                .textBody(body300)
                .build();
        assertThat(testee).isEqualToComparingFieldByField(expected);
    }
    
    @Test(expected=NotImplementedException.class)
    public void attachmentsShouldNotBeHandledForNow() throws Exception {
        org.apache.james.mailbox.store.mail.model.Message<TestId> testMail = new SimpleMessage<>(
                INTERNAL_DATE,
                0,
                0,
                new SharedByteArrayInputStream(IOUtils.toByteArray(ClassLoader.getSystemResourceAsStream("spamMail.eml"))),
                new Flags(Flag.SEEN),
                new PropertyBuilder(),
                MAILBOX_ID);
        testMail.setModSeq(MOD_SEQ);
        
        Message.fromMailboxMessage(testMail);
    }
}
