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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.james.mailbox.MailboxManager;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.user.lib.mock.InMemoryUsersRepository;
import org.junit.Before;
import org.junit.Test;

public class FirstUserConnectionFilterTest {

    private FirstUserConnectionFilter sut;
    private InMemoryUsersRepository usersRepository;

    @Before
    public void setup() {
        usersRepository = new InMemoryUsersRepository();
        MailboxManager mailboxManager = mock(MailboxManager.class);
        sut = new FirstUserConnectionFilter(usersRepository, mailboxManager);
    }
    
    @Test
    public void filterShouldDoNothingOnNullSession() throws IOException, ServletException, UsersRepositoryException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        sut.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        assertThat(usersRepository.list()).isEmpty();
    }
    
}

