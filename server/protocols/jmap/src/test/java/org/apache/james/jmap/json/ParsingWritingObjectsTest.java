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

package org.apache.james.jmap.json;

import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.apache.commons.io.IOUtils;
import org.apache.james.jmap.methods.GetMessagesMethod;
import org.apache.james.jmap.methods.JmapResponseWriterImpl;
import org.apache.james.jmap.model.Emailer;
import org.apache.james.jmap.model.Message;
import org.apache.james.jmap.model.MessageId;
import org.apache.james.jmap.model.SubMessage;
import org.junit.Test;

import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class ParsingWritingObjectsTest {

    @Test
    public void parsingJsonShouldWorkOnSubMessage() throws Exception {
        SubMessage expected = SubMessage.builder()
                .headers(ImmutableMap.of("h1", "h1Value", "h2", "h2Value"))
                .from(Emailer.builder().name("myName").email("myEmail@james.org").build())
                .to(ImmutableList.of(Emailer.builder().name("to1").email("to1@james.org").build(),
                        Emailer.builder().name("to2").email("to2@james.org").build()))
                .cc(ImmutableList.of(Emailer.builder().name("cc1").email("cc1@james.org").build(),
                        Emailer.builder().name("cc2").email("cc2@james.org").build()))
                .bcc(ImmutableList.of(Emailer.builder().name("bcc1").email("bcc1@james.org").build(),
                        Emailer.builder().name("bcc2").email("bcc2@james.org").build()))
                .replyTo(ImmutableList.of(Emailer.builder().name("replyTo1").email("replyTo1@james.org").build(),
                        Emailer.builder().name("replyTo2").email("replyTo2@james.org").build()))
                .subject("mySubject")
                .date(ZonedDateTime.parse("2014-10-30T14:12:00Z").withZoneSameLocal(ZoneId.of("GMT")))
                .textBody("myTextBody")
                .htmlBody("<h1>myHtmlBody</h1>")
                .build();

        SubMessage subMessage = new ObjectMapperFactory().forParsing()
            .readValue(IOUtils.toString(ClassLoader.getSystemResource("json/subMessage.json")), SubMessage.class);

        assertThat(subMessage).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void writingJsonShouldWorkOnSubMessage() throws Exception {
        String expected = IOUtils.toString(ClassLoader.getSystemResource("json/subMessage.json"));

        String json = new ObjectMapperFactory().forWriting()
                .writeValueAsString(SubMessage.builder()
                        .headers(ImmutableMap.of("h1", "h1Value", "h2", "h2Value"))
                        .from(Emailer.builder().name("myName").email("myEmail@james.org").build())
                        .to(ImmutableList.of(Emailer.builder().name("to1").email("to1@james.org").build(),
                                Emailer.builder().name("to2").email("to2@james.org").build()))
                        .cc(ImmutableList.of(Emailer.builder().name("cc1").email("cc1@james.org").build(),
                                Emailer.builder().name("cc2").email("cc2@james.org").build()))
                        .bcc(ImmutableList.of(Emailer.builder().name("bcc1").email("bcc1@james.org").build(),
                                Emailer.builder().name("bcc2").email("bcc2@james.org").build()))
                        .replyTo(ImmutableList.of(Emailer.builder().name("replyTo1").email("replyTo1@james.org").build(),
                                Emailer.builder().name("replyTo2").email("replyTo2@james.org").build()))
                        .subject("mySubject")
                        .date(ZonedDateTime.parse("2014-10-30T14:12:00Z").withZoneSameLocal(ZoneId.of("GMT")))
                        .textBody("myTextBody")
                        .htmlBody("<h1>myHtmlBody</h1>")
                        .build());

        assertThatJson(json)
            .when(IGNORING_ARRAY_ORDER)
            .isEqualTo(expected);

    }

    @Test
    public void parsingJsonShouldWorkOnMessage() throws Exception {
        Message expected = Message.builder()
                .id(MessageId.of("username|mailbox|1"))
                .blobId("myBlobId")
                .threadId("myThreadId")
                .mailboxIds(ImmutableList.of("mailboxId1", "mailboxId2"))
                .inReplyToMessageId("myInReplyToMessageId")
                .isUnread(true)
                .isFlagged(true)
                .isAnswered(true)
                .isDraft(true)
                .hasAttachment(true)
                .headers(ImmutableMap.of("h1", "h1Value", "h2", "h2Value"))
                .from(Emailer.builder().name("myName").email("myEmail@james.org").build())
                .to(ImmutableList.of(Emailer.builder().name("to1").email("to1@james.org").build(),
                        Emailer.builder().name("to2").email("to2@james.org").build()))
                .cc(ImmutableList.of(Emailer.builder().name("cc1").email("cc1@james.org").build(),
                        Emailer.builder().name("cc2").email("cc2@james.org").build()))
                .bcc(ImmutableList.of(Emailer.builder().name("bcc1").email("bcc1@james.org").build(),
                        Emailer.builder().name("bcc2").email("bcc2@james.org").build()))
                .replyTo(ImmutableList.of(Emailer.builder().name("replyTo1").email("replyTo1@james.org").build(),
                        Emailer.builder().name("replyTo2").email("replyTo2@james.org").build()))
                .subject("mySubject")
                .date(ZonedDateTime.parse("2014-10-30T14:12:00Z").withZoneSameLocal(ZoneId.of("GMT")))
                .size(1024)
                .preview("myPreview")
                .textBody("myTextBody")
                .htmlBody("<h1>myHtmlBody</h1>")
                .build();

        Message message = new ObjectMapperFactory().forParsing()
            .readValue(IOUtils.toString(ClassLoader.getSystemResource("json/message.json")), Message.class);

        assertThat(message).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void writingJsonShouldWorkOnMessage() throws Exception {
        String expected = IOUtils.toString(ClassLoader.getSystemResource("json/message.json"));

        SimpleFilterProvider filterProvider = new SimpleFilterProvider()
                .addFilter(JmapResponseWriterImpl.PROPERTIES_FILTER, SimpleBeanPropertyFilter.serializeAll())
                .addFilter(GetMessagesMethod.HEADERS_FILTER, SimpleBeanPropertyFilter.serializeAll());

        String json = new ObjectMapperFactory().forWriting()
                .setFilterProvider(filterProvider)
                .writeValueAsString(Message.builder()
                        .id(MessageId.of("username|mailbox|1"))
                        .blobId("myBlobId")
                        .threadId("myThreadId")
                        .mailboxIds(ImmutableList.of("mailboxId1", "mailboxId2"))
                        .inReplyToMessageId("myInReplyToMessageId")
                        .isUnread(true)
                        .isFlagged(true)
                        .isAnswered(true)
                        .isDraft(true)
                        .hasAttachment(true)
                        .headers(ImmutableMap.of("h1", "h1Value", "h2", "h2Value"))
                        .from(Emailer.builder().name("myName").email("myEmail@james.org").build())
                        .to(ImmutableList.of(Emailer.builder().name("to1").email("to1@james.org").build(),
                                Emailer.builder().name("to2").email("to2@james.org").build()))
                        .cc(ImmutableList.of(Emailer.builder().name("cc1").email("cc1@james.org").build(),
                                Emailer.builder().name("cc2").email("cc2@james.org").build()))
                        .bcc(ImmutableList.of(Emailer.builder().name("bcc1").email("bcc1@james.org").build(),
                                Emailer.builder().name("bcc2").email("bcc2@james.org").build()))
                        .replyTo(ImmutableList.of(Emailer.builder().name("replyTo1").email("replyTo1@james.org").build(),
                                Emailer.builder().name("replyTo2").email("replyTo2@james.org").build()))
                        .subject("mySubject")
                        .date(ZonedDateTime.parse("2014-10-30T14:12:00Z").withZoneSameLocal(ZoneId.of("GMT")))
                        .size(1024)
                        .preview("myPreview")
                        .textBody("myTextBody")
                        .htmlBody("<h1>myHtmlBody</h1>")
                        .build());

        assertThatJson(json)
            .when(IGNORING_ARRAY_ORDER)
            .isEqualTo(expected);

    }
}
