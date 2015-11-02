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
package org.apache.james.jmap.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.jmap.api.AccessTokenManager;
import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.jmap.api.access.AccessTokenRepository;
import org.apache.james.jmap.api.access.exceptions.InvalidAccessToken;
import org.apache.james.jmap.memory.access.MemoryAccessTokenRepository;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

public class AccessTokenManagerImplTest {
    
    private AccessTokenManager accessTokenManager;
    private AccessTokenRepository accessTokenRepository;
    
    @Before
    public void setUp() throws Exception {
        Injector injector = Guice.createInjector(new AbstractModule() {
            
            @Override
            protected void configure() {
                bind(Long.class).annotatedWith(Names.named("tokenExpirationInMs")).toInstance(100L);
                bind(AccessTokenRepository.class).to(MemoryAccessTokenRepository.class);
            }
        });
        accessTokenRepository = injector.getInstance(AccessTokenRepository.class);
        accessTokenManager = new AccessTokenManagerImpl(accessTokenRepository);
    }

    @Test(expected=NullPointerException.class)
    public void grantShouldThrowOnNullUsername() throws Exception {
        accessTokenManager.grantAccessToken(null);
    }
    
    @Test
    public void grantShouldGenerateATokenOnUsername() throws Exception {
        assertThat(accessTokenManager.grantAccessToken("username")).isNotNull();
    }

    @Test
    public void grantShouldStoreATokenOnUsername() throws Exception {
        AccessToken token = accessTokenManager.grantAccessToken("username");
        assertThat(accessTokenRepository.getUsernameFromToken(token)).isEqualTo("username");
    }
    
    @Test(expected=NullPointerException.class)
    public void getUsernameShouldThrowWhenNullToken() throws Exception {
        accessTokenManager.getUsernameFromToken(null);
    }

    @Test(expected=InvalidAccessToken.class)
    public void getUsernameShouldThrowWhenUnknownToken() throws Exception {
        accessTokenManager.getUsernameFromToken(AccessToken.generate());
    }

    @Test(expected=InvalidAccessToken.class)
    public void getUsernameShouldThrowWhenOtherToken() throws Exception {
        accessTokenManager.grantAccessToken("username");
        accessTokenManager.getUsernameFromToken(AccessToken.generate());
    }

    @Test
    public void getUsernameShouldReturnUsernameWhenExistingUsername() throws Exception {
        AccessToken token = accessTokenManager.grantAccessToken("username");
        assertThat(accessTokenManager.getUsernameFromToken(token)).isEqualTo("username");
    }
    
    @Test(expected=NullPointerException.class)
    public void isValidShouldThrowOnNullToken() throws Exception {
        accessTokenManager.isValid(null);
    }
    
    @Test
    public void isValidShouldReturnFalseOnUnknownToken() throws Exception {
        assertThat(accessTokenManager.isValid(AccessToken.generate())).isFalse();
    }
    
    @Test
    public void isValidShouldReturnFalseWhenOtherToken() throws Exception {
        accessTokenManager.grantAccessToken("username");
        assertThat(accessTokenManager.isValid(AccessToken.generate())).isFalse();
    }
    
    @Test
    public void isValidShouldReturnTrueWhenValidToken() throws Exception {
        AccessToken accessToken = accessTokenManager.grantAccessToken("username");
        assertThat(accessTokenManager.isValid(accessToken)).isTrue();
    }
}
