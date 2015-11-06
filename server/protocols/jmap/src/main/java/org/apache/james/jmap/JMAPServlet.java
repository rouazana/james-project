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
package org.apache.james.jmap;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.james.jmap.methods.RequestHandler;
import org.apache.james.jmap.model.AuthenticatedProtocolRequest;
import org.apache.james.jmap.model.ProtocolRequest;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;

public class JMAPServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String JSON_CONTENT_TYPE = "application/json";
    public static final String JSON_CONTENT_TYPE_UTF8 = "application/json; charset=UTF-8";

    private ObjectMapper objectMapper = new ObjectMapper();

    private final RequestHandler requestHandler;
    
    @Inject
    @VisibleForTesting JMAPServlet(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        JmapAuthenticatedRequest request = (JmapAuthenticatedRequest)req;
        try {
            List<Object[]> responses = 
                requestAsJsonStream(request)
                .map(ProtocolRequest::fromProtocolSpecification)
                .map(x -> AuthenticatedProtocolRequest.decorate(x, request))
                .map(requestHandler::handle)
                .map(protocolResponse -> protocolResponse.asProtocolSpecification())
                .collect(Collectors.toList());

            objectMapper.writeValue(resp.getOutputStream(), responses);
        } catch (IOException e) {
            resp.setStatus(SC_BAD_REQUEST);
        }
    }

    private Stream<JsonNode[]> requestAsJsonStream(HttpServletRequest req) throws IOException, JsonParseException, JsonMappingException {
        return Arrays.stream(
                objectMapper.readValue(req.getInputStream(), JsonNode[][].class));
    }
}
