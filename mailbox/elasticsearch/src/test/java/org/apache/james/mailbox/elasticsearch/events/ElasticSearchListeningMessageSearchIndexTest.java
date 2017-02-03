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
package org.apache.james.mailbox.elasticsearch.events;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.io.IOException;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.backends.es.ElasticSearchIndexer;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.MailboxSession.User;
import org.apache.james.mailbox.elasticsearch.json.MessageToElasticSearchJson;
import org.apache.james.mailbox.elasticsearch.search.ElasticSearchSearcher;
import org.apache.james.mailbox.model.TestId;
import org.apache.james.mailbox.model.UpdatedFlags;
import org.apache.james.mailbox.store.mail.MessageMapperFactory;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;

public class ElasticSearchListeningMailboxMessageSearchIndexTest {

    public static final long MODSEQ = 18L;
    private IMocksControl control;

    private ElasticSearchIndexer indexer;
    private ElasticSearchListeningMessageSearchIndex testee;
    
    @Before
    public void setup() throws JsonProcessingException {
        control = createControl();

        MessageMapperFactory mapperFactory = control.createMock(MessageMapperFactory.class);
        MessageToElasticSearchJson messageToElasticSearchJson = control.createMock(MessageToElasticSearchJson.class);
        ElasticSearchSearcher elasticSearchSearcher = control.createMock(ElasticSearchSearcher.class);

        indexer = control.createMock(ElasticSearchIndexer.class);

        List<User> users = anyObject();
        expect(messageToElasticSearchJson.convertToJson(anyObject(MailboxMessage.class), users)).andReturn("json content").anyTimes();
        expect(messageToElasticSearchJson.getUpdatedJsonMessagePart(anyObject(Flags.class), anyLong())).andReturn("json updated content").anyTimes();

        testee = new ElasticSearchListeningMessageSearchIndex(mapperFactory, indexer, elasticSearchSearcher, messageToElasticSearchJson);
    }
    
    @Test
    public void addShouldIndex() throws Exception {
        MailboxSession.User user = control.createMock(MailboxSession.User.class);
        MailboxSession session = control.createMock(MailboxSession.class);
        expect(session.getUser())
            .andReturn(user);

        Mailbox mailbox = control.createMock(Mailbox.class);
        MessageUid messageUid = MessageUid.of(1);
        TestId mailboxId = TestId.of(12);
        expect(mailbox.getMailboxId()).andReturn(mailboxId);
        MailboxMessage message = mockedMessage(messageUid);
        
        IndexResponse expectedIndexResponse = control.createMock(IndexResponse.class);
        expect(indexer.indexMessage(eq(mailboxId.serialize() + ":" + messageUid.asLong()), anyString()))
            .andReturn(expectedIndexResponse);
        
        control.replay();
        testee.add(session, mailbox, message);
        control.verify();
    }

    private MailboxMessage mockedMessage(MessageUid messageId) throws IOException {
        MailboxMessage message = control.createMock(MailboxMessage.class);
        expect(message.getUid()).andReturn(messageId).anyTimes();
        return message;
    }
    
    @Test
    public void addShouldNotPropagateExceptionWhenExceptionOccurs() throws Exception {
        MailboxSession.User user = control.createMock(MailboxSession.User.class);
        MailboxSession session = control.createMock(MailboxSession.class);
        expect(session.getUser())
            .andReturn(user);

        Mailbox mailbox = control.createMock(Mailbox.class);
        
        MessageUid messageUid = MessageUid.of(1);
        TestId mailboxId = TestId.of(12);
        MailboxMessage message = mockedMessage(messageUid);
        expect(mailbox.getMailboxId()).andReturn(mailboxId);
        
        expect(indexer.indexMessage(eq(mailboxId.serialize() + ":" + messageUid.asLong()), anyString()))
            .andThrow(new ElasticsearchException(""));
        
        control.replay();
        testee.add(session, mailbox, message);
        control.verify();
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void deleteShouldWork() throws Exception {
        MailboxSession session = control.createMock(MailboxSession.class);
        Mailbox mailbox = control.createMock(Mailbox.class);
        MessageUid messageUid = MessageUid.of(1);
        TestId mailboxId = TestId.of(12);
        expect(mailbox.getMailboxId()).andReturn(mailboxId);
        
        BulkResponse expectedBulkResponse = control.createMock(BulkResponse.class);
        expect(indexer.deleteMessages(anyObject(List.class)))
            .andReturn(expectedBulkResponse);
        
        control.replay();
        testee.delete(session, mailbox, Lists.newArrayList(messageUid));
        control.verify();
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void deleteShouldWorkWhenMultipleMessageIds() throws Exception {
        MailboxSession session = control.createMock(MailboxSession.class);
        Mailbox mailbox = control.createMock(Mailbox.class);
        MessageUid messageUid1 = MessageUid.of(1);
        MessageUid messageUid2 = MessageUid.of(2);
        MessageUid messageUid3 = MessageUid.of(3);
        MessageUid messageUid4 = MessageUid.of(4);
        MessageUid messageUid5 = MessageUid.of(5);
        TestId mailboxId = TestId.of(12);
        expect(mailbox.getMailboxId()).andReturn(mailboxId).times(5);

        BulkResponse expectedBulkResponse = control.createMock(BulkResponse.class);
        expect(indexer.deleteMessages(anyObject(List.class)))
            .andReturn(expectedBulkResponse);
        
        control.replay();
        testee.delete(session, mailbox, Lists.newArrayList(messageUid1, messageUid2, messageUid3, messageUid4, messageUid5));
        control.verify();
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void deleteShouldNotPropagateExceptionWhenExceptionOccurs() throws Exception {
        MailboxSession session = control.createMock(MailboxSession.class);
        Mailbox mailbox = control.createMock(Mailbox.class);
        MessageUid messageUid = MessageUid.of(1);
        TestId mailboxId = TestId.of(12);
        expect(mailbox.getMailboxId()).andReturn(mailboxId).times(2);
        
        expect(indexer.deleteMessages(anyObject(List.class)))
            .andThrow(new ElasticsearchException(""));
        
        control.replay();
        testee.delete(session, mailbox, Lists.newArrayList(messageUid));
        control.verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void updateShouldWork() throws Exception {
        MailboxSession session = control.createMock(MailboxSession.class);

        Mailbox mailbox = control.createMock(Mailbox.class);

        Flags flags = new Flags();
        MessageUid messageUid = MessageUid.of(1);
        UpdatedFlags updatedFlags = new UpdatedFlags(messageUid, MODSEQ, flags, flags);
        TestId mailboxId = TestId.of(12);

        expectLastCall();
        expect(mailbox.getMailboxId()).andReturn(mailboxId);
        
        BulkResponse expectedBulkResponse = control.createMock(BulkResponse.class);
        expect(indexer.updateMessages(anyObject(List.class)))
            .andReturn(expectedBulkResponse);
        
        control.replay();
        testee.update(session, mailbox, Lists.newArrayList(updatedFlags));
        control.verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void updateShouldNotPropagateExceptionWhenExceptionOccurs() throws Exception {
        MailboxSession session = control.createMock(MailboxSession.class);

        Mailbox mailbox = control.createMock(Mailbox.class);
        Flags flags = new Flags();
        MessageUid messageUid = MessageUid.of(1);
        UpdatedFlags updatedFlags = new UpdatedFlags(messageUid, MODSEQ, flags, flags);
        TestId mailboxId = TestId.of(12);

        expectLastCall();
        expect(mailbox.getMailboxId()).andReturn(mailboxId).times(2);

        expect(indexer.updateMessages(anyObject(List.class)))
            .andThrow(new ElasticsearchException(""));
        
        control.replay();
        testee.update(session, mailbox, Lists.newArrayList(updatedFlags));
        control.verify();
    }

    @Test
    public void deleteAllShouldWork() throws Exception {
        MailboxSession session = control.createMock(MailboxSession.class);

        Mailbox mailbox = control.createMock(Mailbox.class);

        TestId mailboxId = TestId.of(12);

        expectLastCall();
        expect(mailbox.getMailboxId()).andReturn(mailboxId);

        indexer.deleteAllMatchingQuery(anyObject(QueryBuilder.class));
        EasyMock.expectLastCall();

        control.replay();
        testee.deleteAll(session, mailbox);
        control.verify();
    }

    @Test
    public void deleteAllShouldNotPropagateExceptionWhenExceptionOccurs() throws Exception {
        MailboxSession session = control.createMock(MailboxSession.class);

        Mailbox mailbox = control.createMock(Mailbox.class);
        TestId mailboxId = TestId.of(12);

        expectLastCall();
        expect(mailbox.getMailboxId()).andReturn(mailboxId).times(2);

        indexer.deleteAllMatchingQuery(anyObject(QueryBuilder.class));
        EasyMock.expectLastCall().andThrow(new ElasticsearchException(""));

        control.replay();
        testee.deleteAll(session, mailbox);
        control.verify();
    }
}
