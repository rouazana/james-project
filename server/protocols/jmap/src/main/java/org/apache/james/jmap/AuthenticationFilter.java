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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.james.jmap.api.AccessTokenManager;
import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.mailbox.MailboxSession;

public class AuthenticationFilter implements Filter {
    
    private AccessTokenManager accessTokenManager;

    public AuthenticationFilter(AccessTokenManager accessTokenManager) {
        this.accessTokenManager = accessTokenManager;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest)request;
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !checkAuthorizationHeader(authHeader)) {
            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        MailboxSession mailboxSession = fromHeader(authHeader);
        JmapAuthenticatedRequest jmapAuthenticatedRequest = new JmapAuthenticatedRequest(httpRequest, mailboxSession);
        chain.doFilter(jmapAuthenticatedRequest, response);
    }

    private MailboxSession fromHeader(String authHeader) {
        // TODO Auto-generated method stub
        return null;
    }

    private boolean checkAuthorizationHeader(String authHeader) throws IOException {
        return accessTokenManager.isValid(AccessToken.fromString(authHeader));
    }

    @Override
    public void destroy() {
    }

}
