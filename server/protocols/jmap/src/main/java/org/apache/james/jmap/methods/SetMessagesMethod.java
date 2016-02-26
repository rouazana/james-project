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

import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.james.jmap.model.ClientId;
import org.apache.james.jmap.model.SetMessagesRequest;
import org.apache.james.jmap.model.SetMessagesResponse;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.store.mail.model.MailboxId;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

public class SetMessagesMethod<Id extends MailboxId> implements Method {

    private static final Method.Request.Name METHOD_NAME = Method.Request.name("setMessages");
    private static final Method.Response.Name RESPONSE_NAME = Method.Response.name("messagesSet");

    private final Set<SetMessagesProcessor> messagesProcessors;

    @Inject
    @VisibleForTesting SetMessagesMethod(Set<SetMessagesProcessor> messagesProcessors) {
        this.messagesProcessors = messagesProcessors;
    }

    @Override
    public Method.Request.Name requestHandled() {
        return METHOD_NAME;
    }

    @Override
    public Class<? extends JmapRequest> requestType() {
        return SetMessagesRequest.class;
    }

    public Stream<JmapResponse> process(JmapRequest request, ClientId clientId, MailboxSession mailboxSession) {
        Preconditions.checkArgument(request instanceof SetMessagesRequest);
        try {
            return Stream.of(
                    JmapResponse.builder().clientId(clientId)
                    .response(setMessagesResponse((SetMessagesRequest) request, mailboxSession))
                    .responseName(RESPONSE_NAME)
                    .build());
        } catch (MailboxException e) {
            return Stream.of(
                    JmapResponse.builder().clientId(clientId)
                    .error()
                    .responseName(RESPONSE_NAME)
                    .build());
        }
    }

    private SetMessagesResponse setMessagesResponse(SetMessagesRequest request, MailboxSession mailboxSession) throws MailboxException {
        return messagesProcessors.stream()
                .map(processor -> processor.process(request, mailboxSession))
                .reduce(SetMessagesResponse.builder(),
                        (builder, resp) -> resp.mergeInto(builder) ,
                        (builder1, builder2) -> builder2.build().mergeInto(builder1)
                )
                .build();
    }
}
