package org.apache.james.jmap;

import static com.jayway.restassured.RestAssured.with;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.james.jmap.utils.ZonedDateTimeProvider;
import org.apache.james.user.api.UsersRepository;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

public class TestClient {

    private static final ZonedDateTime oldDate = ZonedDateTime.parse("2011-12-03T10:15:30+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    private static final ZonedDateTime newDate = ZonedDateTime.parse("2011-12-03T10:16:30+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    private ZonedDateTimeProvider zonedDateTimeProvider;
    private UsersRepository userRepository;

    public TestClient(TestServer server) {
        RestAssured.port = server.getLocalPort();
        RestAssured.config = newConfig().encoderConfig(encoderConfig().defaultContentCharset("UTF-8"));
        zonedDateTimeProvider = server.getZonedDateTimeProvider();
        userRepository = server.getUserRepository();
    }
    
    public String authenticate() throws Exception {
        when(zonedDateTimeProvider.get())
            .thenReturn(oldDate);

        String continuationToken = fromGoodContinuationTokenRequest();
    
        when(userRepository.test("user@domain.tld", "password"))
            .thenReturn(true);
        when(zonedDateTimeProvider.get())
            .thenReturn(newDate);
    
        return fromGoodAccessTokenRequest(continuationToken);
    }

    private String fromGoodContinuationTokenRequest() {
        return with()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"username\": \"user@domain.tld\", \"clientName\": \"Mozilla Thunderbird\", \"clientVersion\": \"42.0\", \"deviceName\": \"Joe Blogg’s iPhone\"}")
        .post("/authentication")
            .body()
            .path("continuationToken")
            .toString();
    }

    private String fromGoodAccessTokenRequest(String continuationToken) {
        return with()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"token\": \"" + continuationToken + "\", \"method\": \"password\", \"password\": \"password\"}")
        .post("/authentication")
            .path("accessToken")
            .toString();
    }
}