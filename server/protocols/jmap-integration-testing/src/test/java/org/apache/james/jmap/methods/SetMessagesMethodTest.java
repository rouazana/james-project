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

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.with;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsMapWithSize.aMapWithSize;
import static org.hamcrest.collection.IsMapWithSize.anEmptyMap;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.mail.Flags;

import org.apache.james.backends.cassandra.EmbeddedCassandra;
import org.apache.james.jmap.JmapAuthentication;
import org.apache.james.jmap.JmapServer;
import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.mailbox.elasticsearch.EmbeddedElasticSearch;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxPath;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Charsets;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.specification.ResponseSpecification;

public abstract class SetMessagesMethodTest {

    private static final String NAME = "[0][0]";
    private static final String ARGUMENTS = "[0][1]";

    private TemporaryFolder temporaryFolder = new TemporaryFolder();
    private EmbeddedElasticSearch embeddedElasticSearch = new EmbeddedElasticSearch(temporaryFolder);
    private EmbeddedCassandra cassandra = EmbeddedCassandra.createStartServer();
    private JmapServer jmapServer = jmapServer(temporaryFolder, embeddedElasticSearch, cassandra);
    private String outboxId;

    protected abstract JmapServer jmapServer(TemporaryFolder temporaryFolder, EmbeddedElasticSearch embeddedElasticSearch, EmbeddedCassandra cassandra);

    @Rule
    public RuleChain chain = RuleChain
        .outerRule(temporaryFolder)
        .around(embeddedElasticSearch)
        .around(jmapServer);

    private AccessToken accessToken;
    private String username;

    @Before
    public void setup() throws Exception {
        RestAssured.port = jmapServer.getPort();
        RestAssured.config = newConfig().encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8));

        String domain = "domain.tld";
        username = "username@" + domain;
        String password = "password";
        jmapServer.serverProbe().addDomain(domain);
        jmapServer.serverProbe().addUser(username, password);
        jmapServer.serverProbe().createMailbox("#private", "username", "inbox");
        accessToken = JmapAuthentication.authenticateJamesUser(username, password);

        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "outbox");
        embeddedElasticSearch.awaitForElasticSearch();
        // Find the newly created outbox mailbox (using getMailboxes command on /jmap endpoint)
        List<Map<String, String>> mailboxes = with()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMailboxes\", {\"properties\": [\"role\", \"id\"]}, \"#0\"]]")
                .post("/jmap")
                .andReturn()
                .body()
                .jsonPath()
                .getList(ARGUMENTS + ".list");

        outboxId = mailboxes.stream()
                .filter(x -> x.get("role").equals("outbox"))
                .map(x -> x.get("id"))
                .findFirst().get();
    }

    @Test
    public void setMessagesShouldReturnErrorNotSupportedWhenRequestContainsNonNullAccountId() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"accountId\": \"1\"}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("error"))
            .body(ARGUMENTS + ".type", equalTo("Not yet implemented"));
    }

    @Test
    public void setMessagesShouldReturnErrorNotSupportedWhenRequestContainsNonNullIfInState() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"ifInState\": \"1\"}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("error"))
            .body(ARGUMENTS + ".type", equalTo("Not yet implemented"));
    }

    @Test
    public void setMessagesShouldReturnNotDestroyedWhenUnknownMailbox() throws Exception {

        String unknownMailboxMessageId = username + "|unknown|12345";
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"destroy\": [\"" + unknownMailboxMessageId + "\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body(NAME, equalTo("messagesSet"))
            .body(ARGUMENTS + ".destroyed", empty())
            .body(ARGUMENTS + ".notDestroyed", hasEntry(equalTo(unknownMailboxMessageId), Matchers.allOf(
                    hasEntry("type", "anErrorOccurred"),
                    hasEntry("description", "An error occurred while deleting message " + unknownMailboxMessageId),
                    hasEntry(equalTo("properties"), isEmptyOrNullString())))
            );
    }

    @Test
    public void setMessagesShouldReturnNotDestroyedWhenNoMatchingMessage() throws Exception {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        String messageId = username + "|mailbox|12345";
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"destroy\": [\"" + messageId + "\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body(NAME, equalTo("messagesSet"))
            .body(ARGUMENTS + ".destroyed", empty())
            .body(ARGUMENTS + ".notDestroyed", hasEntry(equalTo(messageId), Matchers.allOf(
                    hasEntry("type", "notFound"),
                    hasEntry("description", "The message " + messageId + " can't be found"),
                    hasEntry(equalTo("properties"), isEmptyOrNullString())))
            );
    }

    @Test
    public void setMessagesShouldReturnDestroyedWhenMatchingMessage() throws Exception {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"), 
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        embeddedElasticSearch.awaitForElasticSearch();

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"destroy\": [\"" + username + "|mailbox|1\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messagesSet"))
            .body(ARGUMENTS + ".notDestroyed", anEmptyMap())
            .body(ARGUMENTS + ".destroyed", hasSize(1))
            .body(ARGUMENTS + ".destroyed", contains(username + "|mailbox|1"));
    }

    @Test
    public void setMessagesShouldDeleteMessageWhenMatchingMessage() throws Exception {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"), 
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        embeddedElasticSearch.awaitForElasticSearch();

        // When
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"destroy\": [\"" + username + "|mailbox|1\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        // Then
        given()
           .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessages\", {\"ids\": [\"" + username + "|mailbox|1\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messages"))
            .body(ARGUMENTS + ".list", empty());
    }

    @Test
    public void setMessagesShouldReturnDestroyedNotDestroyWhenMixed() throws Exception {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"), 
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"), 
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"), 
                new ByteArrayInputStream("Subject: test3\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        embeddedElasticSearch.awaitForElasticSearch();

        String missingMessageId = username + "|mailbox|4";
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"destroy\": [\"" + username + "|mailbox|1\", \"" + missingMessageId + "\", \"" + username + "|mailbox|3\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messagesSet"))
            .body(ARGUMENTS + ".destroyed", hasSize(2))
            .body(ARGUMENTS + ".notDestroyed", aMapWithSize(1))
            .body(ARGUMENTS + ".destroyed", contains(username + "|mailbox|1", username + "|mailbox|3"))
            .body(ARGUMENTS + ".notDestroyed", hasEntry(equalTo(missingMessageId), Matchers.allOf(
                    hasEntry("type", "notFound"),
                    hasEntry("description", "The message " + missingMessageId + " can't be found")))
            );
    }

    @Test
    public void setMessagesShouldDeleteMatchingMessagesWhenMixed() throws Exception {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"), 
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"), 
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"), 
                new ByteArrayInputStream("Subject: test3\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        embeddedElasticSearch.awaitForElasticSearch();

        // When
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"destroy\": [\"" + username + "|mailbox|1\", \"" + username + "|mailbox|4\", \"" + username + "|mailbox|3\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        // Then
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessages\", {\"ids\": [\"" + username + "|mailbox|1\", \"" + username + "|mailbox|2\", \"" + username + "|mailbox|3\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messages"))
            .body(ARGUMENTS + ".list", hasSize(1));
    }

    @Test
    public void setMessagesShouldReturnUpdatedIdAndNoErrorWhenIsUnreadPassedToFalse() throws MailboxException {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        embeddedElasticSearch.awaitForElasticSearch();

        String presumedMessageId = username + "|mailbox|1";

        // When
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isUnread\" : false } } }, \"#0\"]]", presumedMessageId))
        .when()
            .post("/jmap")
        // Then
        .then()
            .spec(getSetMessagesUpdateOKResponseAssertions(presumedMessageId))
            .log().ifValidationFails();
    }

    private ResponseSpecification getSetMessagesUpdateOKResponseAssertions(String messageId) {
        ResponseSpecBuilder builder = new ResponseSpecBuilder()
                .expectStatusCode(200)
                .expectBody(NAME, equalTo("messagesSet"))
                .expectBody(ARGUMENTS + ".updated", hasSize(1))
                .expectBody(ARGUMENTS + ".updated", contains(messageId))
                .expectBody(ARGUMENTS + ".error", isEmptyOrNullString())
                .expectBody(ARGUMENTS + ".notUpdated", not(hasKey(messageId)));
        return builder.build();
    }

    @Test
    public void setMessagesShouldMarkAsReadWhenIsUnreadPassedToFalse() throws MailboxException {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        embeddedElasticSearch.awaitForElasticSearch();

        String presumedMessageId = username + "|mailbox|1";
        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isUnread\" : false } } }, \"#0\"]]", presumedMessageId))
        // When
        .when()
                .post("/jmap");
        // Then
        with()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMessages\", {\"ids\": [\"" + presumedMessageId + "\"]}, \"#0\"]]")
                .post("/jmap")
        .then()
                .statusCode(200)
                .body(NAME, equalTo("messages"))
                .body(ARGUMENTS + ".list", hasSize(1))
                .body(ARGUMENTS + ".list[0].isUnread", equalTo(false))
                .log().ifValidationFails();
    }

    @Test
    public void setMessagesShouldReturnUpdatedIdAndNoErrorWhenIsUnreadPassed() throws MailboxException {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags(Flags.Flag.SEEN));
        embeddedElasticSearch.awaitForElasticSearch();

        String presumedMessageId = username + "|mailbox|1";
        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isUnread\" : true } } }, \"#0\"]]", presumedMessageId))
        // When
        .when()
                .post("/jmap")
        // Then
        .then()
                .spec(getSetMessagesUpdateOKResponseAssertions(presumedMessageId))
                .log().ifValidationFails();
    }

    @Test
    public void setMessagesShouldMarkAsUnreadWhenIsUnreadPassed() throws MailboxException {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags(Flags.Flag.SEEN));
        embeddedElasticSearch.awaitForElasticSearch();

        String presumedMessageId = username + "|mailbox|1";
        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isUnread\" : true } } }, \"#0\"]]", presumedMessageId))
        // When
        .when()
                .post("/jmap");
        // Then
        with()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMessages\", {\"ids\": [\"" + presumedMessageId + "\"]}, \"#0\"]]")
                .post("/jmap")
        .then()
                .body(NAME, equalTo("messages"))
                .body(ARGUMENTS + ".list", hasSize(1))
                .body(ARGUMENTS + ".list[0].isUnread", equalTo(true))
                .log().ifValidationFails();
    }


    @Test
    public void setMessagesShouldReturnUpdatedIdAndNoErrorWhenIsFlaggedPassed() throws MailboxException {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        embeddedElasticSearch.awaitForElasticSearch();

        String presumedMessageId = username + "|mailbox|1";
        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isFlagged\" : true } } }, \"#0\"]]", presumedMessageId))
        // When
        .when()
                .post("/jmap")
        // Then
        .then()
                .spec(getSetMessagesUpdateOKResponseAssertions(presumedMessageId))
                .log().ifValidationFails();
    }

    @Test
    public void setMessagesShouldMarkAsFlaggedWhenIsFlaggedPassed() throws MailboxException {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        embeddedElasticSearch.awaitForElasticSearch();

        String presumedMessageId = username + "|mailbox|1";
        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isFlagged\" : true } } }, \"#0\"]]", presumedMessageId))
        // When
        .when()
                .post("/jmap");
        // Then
        with()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMessages\", {\"ids\": [\"" + presumedMessageId + "\"]}, \"#0\"]]")
                .post("/jmap")
        .then()
                .body(NAME, equalTo("messages"))
                .body(ARGUMENTS + ".list", hasSize(1))
                .body(ARGUMENTS + ".list[0].isFlagged", equalTo(true))
                .log().ifValidationFails();
    }

    @Test
    public void setMessagesShouldRejectUpdateWhenPropertyHasWrongType() throws MailboxException {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());

        embeddedElasticSearch.awaitForElasticSearch();

        String messageId = username + "|mailbox|1";

        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isUnread\" : \"123\" } } }, \"#0\"]]", messageId))
        .when()
                .post("/jmap")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))
                .body(ARGUMENTS + ".notUpdated", hasKey(messageId))
                .body(ARGUMENTS + ".notUpdated[\""+messageId+"\"].type", equalTo("invalidProperties"))
                .body(ARGUMENTS + ".notUpdated[\""+messageId+"\"].properties[0]", equalTo("isUnread"))
                .body(ARGUMENTS + ".notUpdated[\""+messageId+"\"].description", equalTo("isUnread: Can not construct instance of java.lang.Boolean from String value '123': only \"true\" or \"false\" recognized\n" +
                        " at [Source: {\"isUnread\":\"123\"}; line: 1, column: 2] (through reference chain: org.apache.james.jmap.model.Builder[\"isUnread\"])"))
                .body(ARGUMENTS + ".updated", hasSize(0));
    }

    @Test
    @Ignore("Jackson json deserializer stops after first error found")
    public void setMessagesShouldRejectUpdateWhenPropertiesHaveWrongTypes() throws MailboxException {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());

        embeddedElasticSearch.awaitForElasticSearch();

        String messageId = username + "|mailbox|1";

        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isUnread\" : \"123\", \"isFlagged\" : 456 } } }, \"#0\"]]", messageId))
        .when()
                .post("/jmap")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))
                .body(ARGUMENTS + ".notUpdated", hasKey(messageId))
                .body(ARGUMENTS + ".notUpdated[\""+messageId+"\"].type", equalTo("invalidProperties"))
                .body(ARGUMENTS + ".notUpdated[\""+messageId+"\"].properties", hasSize(2))
                .body(ARGUMENTS + ".notUpdated[\""+messageId+"\"].properties[0]", equalTo("isUnread"))
                .body(ARGUMENTS + ".notUpdated[\""+messageId+"\"].properties[1]", equalTo("isFlagged"))
                .body(ARGUMENTS + ".updated", hasSize(0));
    }

    @Test
    public void setMessagesShouldMarkMessageAsAnsweredWhenIsAnsweredPassed() throws MailboxException {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        embeddedElasticSearch.awaitForElasticSearch();

        String presumedMessageId = username + "|mailbox|1";
        // When
        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isAnswered\" : true } } }, \"#0\"]]", presumedMessageId))
        .when()
                .post("/jmap")
        // Then
        .then()
                .spec(getSetMessagesUpdateOKResponseAssertions(presumedMessageId))
                .log().ifValidationFails();
    }

    @Test
    public void setMessagesShouldMarkAsAnsweredWhenIsAnsweredPassed() throws MailboxException {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        embeddedElasticSearch.awaitForElasticSearch();

        String presumedMessageId = username + "|mailbox|1";
        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isAnswered\" : true } } }, \"#0\"]]", presumedMessageId))
        // When
        .when()
                .post("/jmap");
        // Then
        with()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMessages\", {\"ids\": [\"" + presumedMessageId + "\"]}, \"#0\"]]")
                .post("/jmap")
        .then()
                .body(NAME, equalTo("messages"))
                .body(ARGUMENTS + ".list", hasSize(1))
                .body(ARGUMENTS + ".list[0].isAnswered", equalTo(true))
                .log().ifValidationFails();
    }

    @Test
    public void setMessageShouldReturnNotFoundWhenUpdateUnknownMessage() {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        String nonExistingMessageId = username + "|mailbox|12345";

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isUnread\" : true } } }, \"#0\"]]", nonExistingMessageId))
        .when()
            .post("/jmap")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body(NAME, equalTo("messagesSet"))
            .body(ARGUMENTS + ".notUpdated", hasKey(nonExistingMessageId))
            .body(ARGUMENTS + ".notUpdated[\""+nonExistingMessageId+"\"].type", equalTo("notFound"))
            .body(ARGUMENTS + ".notUpdated[\""+nonExistingMessageId+"\"].description", equalTo("message not found"))
            .body(ARGUMENTS + ".updated", hasSize(0));
    }

    @Test
    public void setMessageShouldReturnCreatedMessageWhenSendingMessage() {
        String messageCreationId = "user|inbox|1";
        String requestBody = "[" +
                "  [" +
                "    \"setMessages\","+
                "    {" +
                "      \"create\": { \"" + messageCreationId  + "\" : {" +
                "        \"from\": { \"name\": \"MAILER-DEAMON\", \"email\": \"postmaster@example.com\"}," +
                "        \"to\": [{ \"name\": \"BOB\", \"email\": \"someone@example.com\"}]," +
                "        \"subject\": \"Thank you for joining example.com!\"," +
                "        \"textBody\": \"Hello someone, and thank you for joining example.com!\"," +
                "        \"mailboxIds\": [\"" + outboxId + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(requestBody)
        .when()
                .post("/jmap")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))
                .body(ARGUMENTS + ".notCreated", aMapWithSize(0))
                // note that assertions on result message had to be split between
                // string-typed values and boolean-typed value assertions on the same .created entry
                // make sure only one creation has been processed
                .body(ARGUMENTS + ".created", aMapWithSize(1))
                // assert server-set attributes are returned
                .body(ARGUMENTS + ".created", hasEntry(equalTo(messageCreationId), Matchers.allOf(
                        hasEntry(equalTo("id"), not(isEmptyOrNullString())),
                        hasEntry(equalTo("blobId"), not(isEmptyOrNullString())),
                        hasEntry(equalTo("threadId"), not(isEmptyOrNullString())),
                        hasEntry(equalTo("size"), not(isEmptyOrNullString()))
                )))
                // assert that message flags are all unset
                .body(ARGUMENTS + ".created", hasEntry(equalTo(messageCreationId), Matchers.allOf(
                        hasEntry(equalTo("isDraft"), equalTo(false)),
                        hasEntry(equalTo("isUnread"), equalTo(false)),
                        hasEntry(equalTo("isFlagged"), equalTo(false)),
                        hasEntry(equalTo("isAnswered"), equalTo(false))
                )))
                ;
    }

    @Test
    public void setMessagesShouldCreateMessageInOutboxWhenSendingMessage() throws MailboxException {
        // Given
        String messageCreationId = "user|inbox|1";
        String presumedMessageId = "username@domain.tld|outbox|1";
        String messageSubject = "Thank you for joining example.com!";
        String requestBody = "[" +
                "  [" +
                "    \"setMessages\","+
                "    {" +
                "      \"create\": { \"" + messageCreationId  + "\" : {" +
                "        \"from\": { \"name\": \"MAILER-DAEMON\", \"email\": \"postmaster@example.com\"}," +
                "        \"to\": [{ \"name\": \"BOB\", \"email\": \"someone@example.com\"}]," +
                "        \"subject\": \"" + messageSubject + "\"," +
                "        \"textBody\": \"Hello someone, and thank you for joining example.com!\"," +
                "        \"mailboxIds\": [\"" + outboxId + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(requestBody)
        // When
        .when()
                .post("/jmap");

        // Then
        with()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMessages\", {\"ids\": [\"" + presumedMessageId + "\"]}, \"#0\"]]")
        .post("/jmap")
        .then()
                .body(NAME, equalTo("messages"))
                .body(ARGUMENTS + ".list", hasSize(1))
                .body(ARGUMENTS + ".list[0].subject", equalTo(messageSubject))
                .log().ifValidationFails();
    }

    @Test
    public void setMessagesShouldRejectWhenSendingMessageHasNoValidAddress() {
        String messageCreationId = "user|inbox|1";
        String requestBody = "[" +
                "  [" +
                "    \"setMessages\","+
                "    {" +
                "      \"create\": { \"" + messageCreationId  + "\" : {" +
                "        \"from\": { \"name\": \"MAILER-DEAMON\", \"email\": \"postmaster@example.com\"}," +
                "        \"to\": [{ \"name\": \"BOB\", \"email\": \"someone@example.com@example.com\"}]," +
                "        \"cc\": [{ \"name\": \"ALICE\"}]," +
                "        \"subject\": \"Thank you for joining example.com!\"," +
                "        \"textBody\": \"Hello someone, and thank you for joining example.com!\"," +
                "        \"mailboxIds\": [\"" + outboxId + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(requestBody)
                .when()
                .post("/jmap")
                .then()
                .log().all()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))

                .body(ARGUMENTS + ".notCreated", hasKey(messageCreationId))
                .body(ARGUMENTS + ".notCreated[\""+messageCreationId+"\"].type", equalTo("invalidProperties"))
                .body(ARGUMENTS + ".notCreated[\""+messageCreationId+"\"].description", endsWith("no recipient address set"))
                .body(ARGUMENTS + ".created", aMapWithSize(0));
    }
}