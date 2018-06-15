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
package org.apache.james.mailbox.backup;

import static org.apache.james.mailbox.backup.ZipArchiveEntryAssert.assertThatZipEntry;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Enumeration;

import javax.mail.Flags;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.james.junit.TemporaryFolderExtension;
import org.apache.james.junit.TemporaryFolderExtension.TemporaryFolder;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.TestId;
import org.apache.james.mailbox.model.TestMessageId;
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailboxMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.common.collect.ImmutableList;

@ExtendWith(TemporaryFolderExtension.class)
public class ZipperTest {
    private static final MessageId.Factory MESSAGE_ID_FACTORY = new TestMessageId.Factory();
    private static final Charset MESSAGE_CHARSET = StandardCharsets.UTF_8;
    private static final String MESSAGE_CONTENT_1 = "Simple message content";
    private static final SharedByteArrayInputStream CONTENT_STREAM_1 = new SharedByteArrayInputStream(MESSAGE_CONTENT_1.getBytes(MESSAGE_CHARSET));
    private static final String MESSAGE_CONTENT_2 = "Other message content";
    private static final SharedByteArrayInputStream CONTENT_STREAM_2 = new SharedByteArrayInputStream(MESSAGE_CONTENT_2.getBytes(MESSAGE_CHARSET));
    private static final Date SUN_SEP_9TH_2001 = new Date(1000000000000L);
    private static final MessageId MESSAGE_ID_1 = MESSAGE_ID_FACTORY.generate();
    private static final MessageId MESSAGE_ID_2 = MESSAGE_ID_FACTORY.generate();
    private static final int SIZE_1 = 1000;
    private static final int SIZE_2 = 2000;
    private SimpleMailboxMessage MESSAGE_1 = SimpleMailboxMessage.builder()
            .messageId(MESSAGE_ID_1)
            .content(CONTENT_STREAM_1)
            .size(SIZE_1)
            .internalDate(SUN_SEP_9TH_2001)
            .bodyStartOctet(0)
            .flags(new Flags())
            .propertyBuilder(new PropertyBuilder())
            .mailboxId(TestId.of(1L))
            .build();
    private SimpleMailboxMessage MESSAGE_2 = SimpleMailboxMessage.builder()
            .messageId(MESSAGE_ID_2)
            .content(CONTENT_STREAM_2)
            .size(SIZE_2)
            .internalDate(SUN_SEP_9TH_2001)
            .bodyStartOctet(0)
            .flags(new Flags())
            .propertyBuilder(new PropertyBuilder())
            .mailboxId(TestId.of(1L))
            .build();

    private Zipper testee;
    private File destination;

    @BeforeEach
    void beforeEach(TemporaryFolder temporaryFolder) throws Exception {
        testee = new Zipper();
        destination = File.createTempFile("backup-test", ".zip", temporaryFolder.getTempDir());
    }

    @Test
    void archiveShouldWriteEmptyValidArchiveWhenNoMessage() throws Exception {
        testee.archive(ImmutableList.of(), destination);

        try (ZipFile zipFile = new ZipFile(destination)) {
            assertThat(zipFile.getEntries().hasMoreElements()).isFalse();
        }
    }

    @Test
    void archiveShouldWriteOneMessageWhenOne() throws Exception {
        testee.archive(ImmutableList.of(MESSAGE_1), destination);

        try (ZipFile zipFile = new ZipFile(destination)) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            assertThat(entries.hasMoreElements()).isTrue();
            ZipArchiveEntry entry = entries.nextElement();
            assertThat(entries.hasMoreElements()).isFalse();

            assertThatZipEntry(zipFile, entry)
                .hasName(MESSAGE_ID_1.serialize())
                .hasStringContent(MESSAGE_CONTENT_1);
        }
    }

    @Test
    void archiveShouldWriteTwoMessagesWhenTwo() throws Exception {
        testee.archive(ImmutableList.of(MESSAGE_1, MESSAGE_2), destination);

        try (ZipFile zipFile = new ZipFile(destination)) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            assertThat(entries.hasMoreElements()).isTrue();
            ZipArchiveEntry entry1 = entries.nextElement();
            assertThat(entries.hasMoreElements()).isTrue();
            ZipArchiveEntry entry2 = entries.nextElement();
            assertThat(entries.hasMoreElements()).isFalse();


            assertThatZipEntry(zipFile, entry1)
                .hasName(MESSAGE_ID_1.serialize())
                .hasStringContent(MESSAGE_CONTENT_1);

            assertThatZipEntry(zipFile, entry2)
                .hasName(MESSAGE_ID_2.serialize())
                .hasStringContent(MESSAGE_CONTENT_2);
        }
    }
}
