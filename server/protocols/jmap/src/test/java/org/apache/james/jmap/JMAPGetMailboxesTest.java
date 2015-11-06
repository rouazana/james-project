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

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.james.jmap.utils.ZonedDateTimeProvider;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.model.MailboxId;
import org.apache.james.user.api.UsersRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.jayway.restassured.http.ContentType;

public class JMAPGetMailboxesTest {

    private TestServer server;
    private TestClient client;

    private UsersRepository mockedUsersRepository;
    private ZonedDateTimeProvider mockedZonedDateTimeProvider;
    private MailboxManager mockedMailboxManager;
    private MailboxMapper<MailboxId> mockedMailboxMapper;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws Exception {
        mockedUsersRepository = mock(UsersRepository.class);
        mockedZonedDateTimeProvider = mock(ZonedDateTimeProvider.class);
        mockedMailboxManager = mock(MailboxManager.class);
        mockedMailboxMapper = mock(MailboxMapper.class);

        server = new TestServer(mockedUsersRepository, mockedZonedDateTimeProvider, mockedMailboxManager, mockedMailboxMapper);
        server.start();

        client = new TestClient(server);
    }

    @After
    public void teardown() throws Exception {
        server.stop();
    }
    
    @Test
    public void getMailboxesShouldErrorNotSupportedWhenRequestContainsNonNullAccountId() throws Exception {
        String accessToken = client.authenticate();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken)
            .body("[[\"getMailboxes\", {\"accountId\": \"1\"}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .content(equalTo("[[\"error\",{\"type\":\"Not yet implemented\"},\"#0\"]]"));
    }

    
    @Test
    public void getMailboxesShouldErrorNotSupportedWhenRequestContainsNonNullIds() throws Exception {
        String accessToken = client.authenticate();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken)
            .body("[[\"getMailboxes\", {\"ids\": []}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .content(equalTo("[[\"error\",{\"type\":\"Not yet implemented\"},\"#0\"]]"));
    }
    
    @Test
    public void getMailboxesShouldErrorNotSupportedWhenRequestContainsNonNullProperties() throws Exception {
        String accessToken = client.authenticate();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken)
            .body("[[\"getMailboxes\", {\"properties\": []}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .content(equalTo("[[\"error\",{\"type\":\"Not yet implemented\"},\"#0\"]]"));
    }
    
    @Test
    public void getMailboxesShouldErrorInvalidArgumentsWhenRequestIsInvalid() throws Exception {
        String accessToken = client.authenticate();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken)
            .body("[[\"getMailboxes\", {\"ids\": true}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .content(equalTo("[[\"error\",{\"type\":\"invalidArguments\"},\"#0\"]]"));
    }

    @Test
    public void getMailboxesShouldReturnEmptyListWhenNoMailboxes() throws Exception {
        when(mockedMailboxManager.list(any()))
            .thenReturn(ImmutableList.<MailboxPath>of());
        String accessToken = client.authenticate();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken)
            .body("[[\"getMailboxes\", {}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .content(equalTo("[[\"getMailboxes\",{\"accountId\":null,\"state\":null,\"list\":[],\"notFound\":null},\"#0\"]]"));
    }

    @Test
    public void getMailboxesShouldReturnMailboxesWhenAvailable() throws Exception {
        when(mockedMailboxManager.list(any()))
            .thenReturn(ImmutableList.<MailboxPath>of(new MailboxPath("namespace", "user", "name"), new MailboxPath("namespace2", "user2", "name2")));
        String accessToken = client.authenticate();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken)
            .body("[[\"getMailboxes\", {}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .content(equalTo("[[\"getMailboxes\",{\"accountId\":null,\"state\":null,\"list\":[],\"notFound\":null},\"#0\"]]"));
    }
}
