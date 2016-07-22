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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.james.jmap.model.ClientId;
import org.apache.james.jmap.model.GetMessageListRequest;
import org.apache.james.jmap.model.GetMessageListResponse;
import org.apache.james.jmap.model.GetMessagesRequest;
import org.apache.james.jmap.model.MessageId;
import org.apache.james.jmap.utils.FilterToSearchQuery;
import org.apache.james.jmap.utils.MailboxUtils;
import org.apache.james.jmap.utils.SortToComparatorConvertor;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.model.FetchGroupImpl;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.model.MessageResult;
import org.apache.james.mailbox.model.MultimailboxesSearchQuery;
import org.apache.james.mailbox.model.SearchQuery;
import org.apache.james.mailbox.store.search.MessageSearchIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

public class GetMessageListMethod implements Method {

    public static final String MAXIMUM_LIMIT = "maximumLimit";
    public static final int DEFAULT_MAXIMUM_LIMIT = 256;

    private static final Logger LOGGER = LoggerFactory.getLogger(GetMailboxesMethod.class);
    private static final Method.Request.Name METHOD_NAME = Method.Request.name("getMessageList");
    private static final Method.Response.Name RESPONSE_NAME = Method.Response.name("messageList");

    private final MailboxManager mailboxManager;
    private final MessageSearchIndex messageSearchIndex;
    private final int maximumLimit;
    private final GetMessagesMethod getMessagesMethod;
    private final MailboxUtils mailboxUtils;

    @Inject
    @VisibleForTesting public GetMessageListMethod(MailboxManager mailboxManager, MessageSearchIndex messageSearchIndex,
            @Named(MAXIMUM_LIMIT) int maximumLimit, GetMessagesMethod getMessagesMethod, MailboxUtils mailboxUtils) {

        this.mailboxManager = mailboxManager;
        this.messageSearchIndex = messageSearchIndex;
        this.maximumLimit = maximumLimit;
        this.getMessagesMethod = getMessagesMethod;
        this.mailboxUtils = mailboxUtils;
    }

    @Override
    public Method.Request.Name requestHandled() {
        return METHOD_NAME;
    }

    @Override
    public Class<? extends JmapRequest> requestType() {
        return GetMessageListRequest.class;
    }

    @Override
    public Stream<JmapResponse> process(JmapRequest request, ClientId clientId, MailboxSession mailboxSession) {
        Preconditions.checkArgument(request instanceof GetMessageListRequest);
        GetMessageListRequest messageListRequest = (GetMessageListRequest) request;
        GetMessageListResponse messageListResponse = getMessageListResponse(messageListRequest, clientId, mailboxSession);
 
        Stream<JmapResponse> jmapResponse = Stream.of(JmapResponse.builder().clientId(clientId)
                .response(messageListResponse)
                .responseName(RESPONSE_NAME)
                .build());
        return Stream.<JmapResponse> concat(jmapResponse, 
                processGetMessages(messageListRequest, messageListResponse, clientId, mailboxSession));
    }

    private GetMessageListResponse getMessageListResponse(GetMessageListRequest messageListRequest, ClientId clientId, MailboxSession mailboxSession) {
        GetMessageListResponse.Builder builder = GetMessageListResponse.builder();
        try {
            MultimailboxesSearchQuery searchQuery = convertToSearchQuery(messageListRequest);
            Map<MailboxId, Collection<Long>> searchResults = messageSearchIndex.search(mailboxSession, searchQuery);
            
            aggregateResults(mailboxSession, searchResults).entries().stream()
                .sorted(comparatorFor(messageListRequest))
                .map(entry -> new MessageId(mailboxSession.getUser(), entry.getKey(), entry.getValue().getUid()))
                .skip(messageListRequest.getPosition())
                .limit(limit(messageListRequest.getLimit()))
                .forEach(builder::messageId);

            return builder.build();
        } catch (MailboxException e) {
            throw Throwables.propagate(e);
        }
    }

    private Multimap<MailboxPath, MessageResult> aggregateResults(MailboxSession mailboxSession, Map<MailboxId, Collection<Long>> searchResults) {
        Multimap<MailboxPath, MessageResult> messages = LinkedHashMultimap.create();
        for (Map.Entry<MailboxId, Collection<Long>> mailboxResults: searchResults.entrySet()) {
            try {
                aggregate(mailboxSession, messages, mailboxResults);
            } catch (MailboxNotFoundException e) {
                LOGGER.error("Error retrieving mailbox", e);
                throw Throwables.propagate(e);
            }
        }
        return messages;
    }

    private void aggregate(MailboxSession mailboxSession, Multimap<MailboxPath, MessageResult> aggregation, Map.Entry<MailboxId, Collection<Long>> mailboxResults) throws MailboxNotFoundException {
        MailboxPath mailboxPath = mailboxUtils.mailboxPathFromMailboxId(mailboxResults.getKey().serialize(), mailboxSession)
            .orElseThrow(() -> new MailboxNotFoundException(mailboxResults.getKey().serialize()));
        MessageManager messageManager = getMessageManager(mailboxPath, mailboxSession)
            .orElseThrow(() -> new MailboxNotFoundException(mailboxPath));
        List<MessageResult> mailboxMessages = MessageRange.toRanges(mailboxResults.getValue()).stream()
            .map(Throwing.function(range -> messageManager.getMessages(range, FetchGroupImpl.MINIMAL, mailboxSession)))
            .map(messageIterator -> ImmutableList.copyOf(messageIterator))
            .flatMap(List::stream)
            .collect(Guavate.toImmutableList());
        aggregation.putAll(mailboxPath, mailboxMessages);
    }

    private MultimailboxesSearchQuery convertToSearchQuery(GetMessageListRequest messageListRequest) {
        SearchQuery searchQuery = messageListRequest.getFilter()
                .map(filter -> new FilterToSearchQuery().convert(filter))
                .orElse(new SearchQuery());
        return MultimailboxesSearchQuery
                .from(searchQuery)
                .build();
    }

    private Stream<JmapResponse> processGetMessages(GetMessageListRequest messageListRequest, GetMessageListResponse messageListResponse, ClientId clientId, MailboxSession mailboxSession) {
        if (shouldChainToGetMessages(messageListRequest)) {
            GetMessagesRequest getMessagesRequest = GetMessagesRequest.builder()
                    .ids(messageListResponse.getMessageIds())
                    .properties(messageListRequest.getFetchMessageProperties())
                    .build();
            return getMessagesMethod.process(getMessagesRequest, clientId, mailboxSession);
        }
        return Stream.empty();
    }

    private boolean shouldChainToGetMessages(GetMessageListRequest messageListRequest) {
        return messageListRequest.isFetchMessages().orElse(false) 
                && !messageListRequest.isFetchThreads().orElse(false);
    }

    private long limit(Optional<Integer> limit) {
        return limit.orElse(maximumLimit);
    }

    private Comparator<Map.Entry<MailboxPath, MessageResult>> comparatorFor(GetMessageListRequest messageListRequest) {
        return SortToComparatorConvertor.comparatorFor(messageListRequest.getSort());
    }

    private Optional<MessageManager> getMessageManager(MailboxPath mailboxPath, MailboxSession mailboxSession) {
        try {
            return Optional.of(mailboxManager.getMailbox(mailboxPath, mailboxSession));
        } catch (MailboxException e) {
            LOGGER.warn("Error retrieveing mailbox :" + mailboxPath, e);
            return Optional.empty();
        }
    }

}
