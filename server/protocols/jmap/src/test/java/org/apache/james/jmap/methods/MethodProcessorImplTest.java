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

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.apache.james.jmap.model.ProtocolRequest;
import org.apache.james.jmap.model.ProtocolResponse;
import org.apache.onami.test.OnamiRunner;
import org.apache.onami.test.annotation.GuiceModules;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;

@RunWith(OnamiRunner.class)
@GuiceModules({ TestMethodsModule.class })
public class MethodProcessorImplTest {

    @Inject
    private MethodProcessor methodProcessor;

    @Test(expected=IllegalStateException.class)
    public void processShouldThrowWhenUnknownMethod() {
        JsonNode[] nodes = new JsonNode[] { new ObjectNode(new JsonNodeFactory(false)).textNode("getAccounts"),
                new ObjectNode(new JsonNodeFactory(false)).putObject("{\"id\": \"id\"}"),
                new ObjectNode(new JsonNodeFactory(false)).textNode("#1")} ;

        MethodProcessorImpl requestHandlerImpl = new MethodProcessorImpl(ImmutableSet.of());
        requestHandlerImpl.process(ProtocolRequest.deserialize(nodes));
    }

    @Test
    public void processShouldWorkWhenKnownMethod() {
        ObjectNode parameters = new ObjectNode(new JsonNodeFactory(false));
        parameters.put("id", "myId");
        parameters.put("name", "myName");
        
        JsonNode[] nodes = new JsonNode[] { new ObjectNode(new JsonNodeFactory(false)).textNode("myMethod"),
                parameters,
                new ObjectNode(new JsonNodeFactory(false)).textNode("#1")} ;

        ProtocolResponse response = methodProcessor.process(ProtocolRequest.deserialize(nodes));

        assertThat(response.getResults().findValue("id").asText()).isEqualTo("myId");
        assertThat(response.getResults().findValue("name").asText()).isEqualTo("myName");
        assertThat(response.getResults().findValue("message").asText()).isEqualTo("works");
    }
}
