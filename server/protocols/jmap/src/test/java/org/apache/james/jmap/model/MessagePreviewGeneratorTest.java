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

package org.apache.james.jmap.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.apache.james.jmap.utils.HtmlTextExtractor;
import org.junit.Before;
import org.junit.Test;

public class MessagePreviewGeneratorTest {
    
    private MessagePreviewGenerator testee;
    private HtmlTextExtractor htmlTextExtractor;
    
    @Before
    public void setUp() {
        testee = new MessagePreviewGenerator(htmlTextExtractor);
    }

    @Test
    public void forPreviewShouldReturnTruncatedStringWithoutHtmlTagWhenStringContainTagsAndIsLongerThan256Characters() {
        //Given
        String body = "This is a <b>HTML</b> mail containing <u>underlined part</u>, <i>italic part</i> and <u><i>underlined AND italic part</i></u>9999999999"
                + "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999"
                + "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999"
                + "000000000011111111112222222222333333333344444444445555555";
        String bodyWithoutTags = "This is a HTML mail containing underlined part, italic part and underlined AND italic part9999999999"
                + "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999"
                + "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999"
                + "000000000011111111112222222222333333333344444444445555555";
        String expected = "This is a HTML mail containing underlined part, italic part and underlined AND italic part9999999999"
                + "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999"
                + "00000000001111111111222222222233333333334444444444555...";
        //When
        when(htmlTextExtractor.toPlainText(body))
            .thenReturn(bodyWithoutTags);
        String actual = testee.forPreview(testee.fromContent(Optional.of(body), Optional.empty()));
        //Then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void forPreviewShouldReturnStringContainingEmptyWhenEmptyStringContent() {
        //Given
        String body = "" ;
        String expected = "(Empty)" ;
        //When
        when(htmlTextExtractor.toPlainText(body))
            .thenReturn(expected);
        String actual = testee.forPreview(testee.fromContent(Optional.of(body), Optional.empty()));
        //Then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void forPreviewShouldReturnTruncatedStringWhenStringContainTagsAndIsLongerThan256Characters() {
        //Given
        String body = "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999"
                + "This is a <b>HTML</b> mail containing <u>underlined part</u>, <i>italic part</i>88888888889999999999"
                + "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999"
                + "000000000011111111112222222222333333333344444444445555555";
        String expected = "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999"
                + "This is a <b>HTML</b> mail containing <u>underlined part</u>, <i>italic part</i>88888888889999999999"
                + "00000000001111111111222222222233333333334444444444555...";
        //When
        String actual = testee.forPreview(testee.fromContent(Optional.empty(), Optional.of(body)));
        //Then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void forPreviewShouldReturnStringContainingEmptyWhenEmptyContent() {
        //Given
        String expected = "(Empty)" ;
        //When
        String actual = testee.forPreview(testee.fromContent(Optional.empty(), Optional.empty()));
        //Then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void asTextShouldReturnStringWithoutHtmlTag() {
        //Given
        String body = "This is a <b>HTML</b> mail";
        String expected = "This is a HTML mail";
        //When
        when(htmlTextExtractor.toPlainText(body))
            .thenReturn(expected);
        Optional<String> actual = testee.asText(body);
        //Then
        assertThat(actual)
            .isPresent()
            .contains(expected);
    }

    @Test
    public void asTextShouldEmptyWhenNullString () {
        assertThat(testee.asText(null)).isEmpty();
    }

    @Test
    public void asTextShouldReturnEmptyStringWhenEmptyString() {
        //Given
        String body = "";
        String expected = "";
        //When
        when(htmlTextExtractor.toPlainText(body))
            .thenReturn(expected);
        Optional<String> actual = testee.asText(body);
        //Then
        assertThat(actual)
            .isPresent()
            .contains(expected);
    }

    @Test
    public void abbreviateShouldNotTruncateAbodyWith256Length() {
        //Given
        String body256 = "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999"
                + "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999"
                + "00000000001111111111222222222233333333334444444444555555";
        //When
        Optional<String> actual = testee.abbreviate(body256);
        //Then
        assertThat(body256.length()).isEqualTo(256);
        assertThat(actual)
            .isPresent()
            .contains(body256);
    }

    @Test
    public void abbreviateShouldTruncateAbodyWith257Length() {
        //Given
        String body257 = "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999"
                + "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999"
                + "000000000011111111112222222222333333333344444444445555555";
        String expected = "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999"
                + "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999"
                + "00000000001111111111222222222233333333334444444444555...";
        //When
        Optional<String> actual = testee.abbreviate(body257);
        //Then
        assertThat(body257.length()).isEqualTo(257);
        assertThat(expected.length()).isEqualTo(256);
        assertThat(actual)
            .isPresent()
            .contains(expected);
    }

    @Test
    public void abbreviateShouldReturnNullStringWhenNullString() {
        //Given
        String body = null;
        String expected = null;
        //When
        Optional<String> actual = testee.abbreviate(body);
        //Then
        assertThat(actual).isEmpty();
    }

    @Test
    public void abbreviateShouldReturnEmptyStringWhenEmptyString() {
        //Given
        String body = "";
        String expected = "";
        //When
        Optional<String> actual = testee.abbreviate(body);
        //Then
        assertThat(actual)
            .isPresent()
            .contains(expected);
    }

    @Test
    public void fromContentShouldReturnEmptyWhenEmptyContent() throws Exception {
        assertThat(testee.fromContent(Optional.empty(), Optional.empty()))
            .isEmpty();
    }

    @Test
    public void fromContentShouldReturnHtmlContentWhenHtmlBody() throws Exception {
        String htmlBody = "<a>HTML</a> content";
        String expected = "HTML content";

        when(htmlTextExtractor.toPlainText(htmlBody))
            .thenReturn(expected);

        assertThat(testee.fromContent(Optional.of(htmlBody), Optional.empty()))
            .isPresent()
            .contains(expected);
    }

    @Test
    public void fromContentShouldReturnHtmlContentWhenHtmlBodyAndTextBody() throws Exception {
        String htmlBody = "<a>HTML</a> content";
        String expected = "HTML content";

        when(htmlTextExtractor.toPlainText(htmlBody))
            .thenReturn(expected);

        assertThat(testee.fromContent(Optional.of(htmlBody), Optional.of("Any text body")))
            .isPresent()
            .contains(expected);
    }

    @Test
    public void fromContentShouldReturnTextContentWhenEmptyHtmlBody() throws Exception {
        String expected = "Any text body";

        assertThat(testee.fromContent(Optional.empty(), Optional.of(expected)))
            .isPresent()
            .contains(expected);
    }
}
