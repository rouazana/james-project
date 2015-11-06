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

import java.io.IOException;

import javax.inject.Inject;

import org.apache.commons.lang.NotImplementedException;
import org.apache.james.jmap.model.GetMailboxesRequest;
import org.apache.james.jmap.model.ProtocolRequest;
import org.apache.james.jmap.model.ProtocolResponse;

import com.google.common.annotations.VisibleForTesting;

public class GetMailboxesMethod implements Method {
    
    private ProtocolArgumentsManager protocolArgumentsManager;

    @Inject
    @VisibleForTesting public GetMailboxesMethod(ProtocolArgumentsManager protocolArgumentsManager) {
        this.protocolArgumentsManager = protocolArgumentsManager;
    }

    public String methodName() {
        return "getMailboxes";
    }

    public ProtocolResponse process(ProtocolRequest request) {
        try {
            protocolArgumentsManager.extractJmapRequest(request, GetMailboxesRequest.class);
        } catch (IOException e) {
            if (e.getCause() instanceof NotImplementedException) {
                return protocolArgumentsManager.formatErrorResponse(request, "Not yet implemented");
            } else {
                return protocolArgumentsManager.formatErrorResponse(request, "invalidArguments");
            }
        }
        return protocolArgumentsManager.formatErrorResponse(request);
    }

}
