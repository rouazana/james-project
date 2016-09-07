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
package org.apache.james.imap.encode;

import java.io.IOException;
import java.util.List;

import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.message.IdRange;
import org.apache.james.imap.api.message.UidRange;
import org.apache.james.imap.api.message.request.SearchResultOption;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.encode.base.AbstractChainedImapEncoder;
import org.apache.james.imap.message.response.ESearchResponse;

/**
 * Encoders IMAP4rev1 <code>ESEARCH</code> responses.
 */
public class ESearchResponseEncoder extends AbstractChainedImapEncoder {

    public ESearchResponseEncoder(ImapEncoder next) {
        super(next);
    }

    protected void doEncode(ImapMessage acceptableMessage, ImapResponseComposer composer, ImapSession session) throws IOException {
        ESearchResponse response = (ESearchResponse) acceptableMessage;
        String tag = response.getTag();
        long min = response.getMinUid();
        long max = response.getMaxUid();
        long count = response.getCount();
        IdRange[] all = response.getAll();
        UidRange[] allUids = response.getAllUids();
        boolean useUid = response.getUseUid();
        Long highestModSeq = response.getHighestModSeq();
        List<SearchResultOption> options = response.getSearchResultOptions();
        
        composer.untagged().message("ESEARCH").openParen().message("TAG").quote(tag).closeParen();
        if (useUid) {
            composer.message("UID");
        }
        if (min > -1 && options.contains(SearchResultOption.MIN)) {
            composer.message(SearchResultOption.MIN.name()).message(min);
        }
        if (max > -1 && options.contains(SearchResultOption.MAX)) {
            composer.message(SearchResultOption.MAX.name()).message(max);
        }
        if (options.contains(SearchResultOption.COUNT)) {
            composer.message(SearchResultOption.COUNT.name()).message(count);
        }
        if (!useUid && all != null && all.length > 0 && options.contains(SearchResultOption.ALL)) {
            composer.message(SearchResultOption.ALL.name());
            composer.sequenceSet(all);
        }
        if (useUid && allUids != null && allUids.length > 0 && options.contains(SearchResultOption.ALL)) {
            composer.message(SearchResultOption.ALL.name());
            composer.sequenceSet(allUids);
        }
        
        // Add the MODSEQ to the response if needed. 
        //
        // see RFC4731 3.2.  Interaction with CONDSTORE extension
        if (highestModSeq != null) {
            composer.message("MODSEQ");
            composer.message(highestModSeq);
        }
        composer.end();
    }

    /**
     * @see org.apache.james.imap.encode.base.AbstractChainedImapEncoder#isAcceptable
     * (org.apache.james.imap.api.ImapMessage)
     */
    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof ESearchResponse);
    }
}
