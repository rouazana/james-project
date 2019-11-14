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

package org.apache.james.user.memory;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.james.core.Domain;
import org.apache.james.core.Username;
import org.apache.james.dnsservice.api.InMemoryDNSService;
import org.apache.james.domainlist.memory.MemoryDomainList;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.user.lib.AbstractUsersRepository;
import org.apache.james.user.lib.AbstractUsersRepositoryTest;
import org.junit.Before;
import org.junit.Test;

public class MemoryUsersRepositoryTest extends AbstractUsersRepositoryTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }
    
    @Override
    protected AbstractUsersRepository getUsersRepository() {
        return MemoryUsersRepository.withVirtualHosting();
    }

    @Test
    public void assertValidShouldThrowWhenDomainPartAndNoVirtualHosting() {
        MemoryUsersRepository memoryUsersRepository = MemoryUsersRepository.withoutVirtualHosting();

        assertThatThrownBy(() -> memoryUsersRepository.assertValid(Username.of("user@domain.tld")))
            .isInstanceOf(UsersRepositoryException.class);
    }

    @Test
    public void assertValidShouldThrowWhenNoDomainPartAndVirtualHosting() {
        MemoryUsersRepository memoryUsersRepository = MemoryUsersRepository.withVirtualHosting();

        assertThatThrownBy(() -> memoryUsersRepository.assertValid(Username.of("user")))
            .isInstanceOf(UsersRepositoryException.class);
    }

    @Test
    public void assertValidShouldNotThrowWhenDomainPartAndVirtualHosting() throws Exception {
        MemoryUsersRepository memoryUsersRepository = MemoryUsersRepository.withVirtualHosting();

        MemoryDomainList domainList = new MemoryDomainList(new InMemoryDNSService()
            .registerMxRecord("localhost", "127.0.0.1")
            .registerMxRecord("127.0.0.1", "127.0.0.1"));
        domainList.setAutoDetect(false);
        domainList.setAutoDetectIP(false);
        domainList.addDomain(Domain.of("domain.tld"));
        memoryUsersRepository.setDomainList(domainList);

        assertThatCode(() -> memoryUsersRepository.assertValid(Username.of("user@domain.tld")))
            .doesNotThrowAnyException();
    }

    @Test
    public void assertValidShouldNotThrowWhenDomainPartAndDomainNotFound() throws Exception {
        MemoryUsersRepository memoryUsersRepository = MemoryUsersRepository.withVirtualHosting();

        MemoryDomainList domainList = new MemoryDomainList(new InMemoryDNSService()
            .registerMxRecord("localhost", "127.0.0.1")
            .registerMxRecord("127.0.0.1", "127.0.0.1"));
        domainList.setAutoDetect(false);
        domainList.setAutoDetectIP(false);
        memoryUsersRepository.setDomainList(domainList);

        assertThatThrownBy(() -> memoryUsersRepository.assertValid(Username.of("user@domain.tld")))
            .isInstanceOf(UsersRepositoryException.class);
    }

    @Test
    public void assertValidShouldNotThrowWhenNoDomainPartAndNoVirtualHosting() {
        MemoryUsersRepository memoryUsersRepository = MemoryUsersRepository.withoutVirtualHosting();

        assertThatCode(() -> memoryUsersRepository.assertValid(Username.of("user")))
            .doesNotThrowAnyException();
    }
}
