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

import org.apache.james.jmap.model.CreationMessage;
import org.apache.james.jmap.model.CreationMessageId;
import org.apache.james.mailbox.store.mail.model.MailboxId;

public class MessageWithId<T> {

    private CreationMessageId creationId;
    private T message;

    public MessageWithId(CreationMessageId creationId, T message) {
        this.creationId = creationId;
        this.message = message;
    }

    public CreationMessageId getCreationId() {
        return creationId;
    }

    public T getMessage() {
        return message;
    }

    public static class CreationMessageEntry<Id extends MailboxId> extends MessageWithId<CreationMessage<Id>> {
        public CreationMessageEntry(CreationMessageId creationId, CreationMessage<Id> message) {
            super(creationId, message);
        }
    }

}
