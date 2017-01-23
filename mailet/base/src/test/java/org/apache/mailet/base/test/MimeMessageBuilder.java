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

package org.apache.mailet.base.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

public class MimeMessageBuilder {

    public static class Header {
        private final String name;
        private final String value;

        public Header(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    public static class MultipartBuilder {
        private ImmutableList.Builder<BodyPart> bodyParts = ImmutableList.builder();

        public MultipartBuilder addBody(BodyPart bodyPart) {
            this.bodyParts.add(bodyPart);
            return this;
        }

        public MultipartBuilder addBodies(BodyPart... bodyParts) {
            this.bodyParts.addAll(Arrays.asList(bodyParts));
            return this;
        }

        public MimeMultipart build() throws MessagingException {
            MimeMultipart multipart = new MimeMultipart();
            List<BodyPart> bodyParts = this.bodyParts.build();
            for(BodyPart bodyPart : bodyParts) {
                multipart.addBodyPart(bodyPart);
            }
            return multipart;
        }
    }

    public static class BodyPartBuilder {
        public static final String DEFAULT_TEXT_PLAIN_UTF8_TYPE = "text/plain; charset=UTF-8";
        public static final String DEFAULT_VALUE = "";

        private Optional<String> cid = Optional.absent();
        private Optional<String> filename = Optional.absent();
        private ImmutableList.Builder<Header> headers = ImmutableList.builder();
        private Optional<String> disposition = Optional.absent();
        private Optional<String> dataAsString = Optional.absent();
        private Optional<byte[]> dataAsBytes = Optional.absent();
        private Optional<String> type = Optional.absent();

        public BodyPartBuilder cid(String cid) {
            this.cid = Optional.of(cid);
            return this;
        }

        public BodyPartBuilder filename(String filename) {
            this.filename = Optional.of(filename);
            return this;
        }

        public BodyPartBuilder disposition(String disposition) {
            this.disposition = Optional.of(disposition);
            return this;
        }

        public BodyPartBuilder data(String data) {
            this.dataAsString = Optional.of(data);
            return this;
        }

        public BodyPartBuilder data(byte[] data) {
            this.dataAsBytes = Optional.of(data);
            return this;
        }

        public BodyPartBuilder type(String type) {
            this.type = Optional.of(type);
            return this;
        }

        public BodyPartBuilder addHeader(String name, String value) {
            this.headers.add(new Header(name, value));
            return this;
        }

        public BodyPartBuilder addHeaders(Header... headers) {
            this.headers.addAll(Arrays.asList(headers));
            return this;
        }

        public BodyPart build() throws IOException, MessagingException {
            Preconditions.checkState(!(dataAsString.isPresent() && dataAsBytes.isPresent()), "Can not specify data as bytes and data as string at the same time");
            MimeBodyPart bodyPart = new MimeBodyPart();
            if (dataAsBytes.isPresent()) {
                bodyPart.setDataHandler(
                    new DataHandler(
                        new ByteArrayDataSource(
                            dataAsBytes.get(),
                            type.or(DEFAULT_TEXT_PLAIN_UTF8_TYPE))
                    ));
            } else {
                bodyPart.setDataHandler(
                    new DataHandler(
                        new ByteArrayDataSource(
                            dataAsString.or(DEFAULT_VALUE),
                            type.or(DEFAULT_TEXT_PLAIN_UTF8_TYPE))
                    ));
            }
            if (filename.isPresent()) {
                bodyPart.setFileName(filename.get());
            }
            if (cid.isPresent()) {
                bodyPart.setContentID(cid.get());
            }
            if (disposition.isPresent()) {
                bodyPart.setDisposition(disposition.get());
            }
            List<Header> headerList = headers.build();
            for(Header header: headerList) {
                bodyPart.addHeader(header.name, header.value);
            }
            return bodyPart;
        }
    }

    public static final Function<String, InternetAddress> TO_INTERNET_ADDRESS = new Function<String, InternetAddress>() {
        @Override
        public InternetAddress apply(String input) {
            try {
                return new InternetAddress(input);
            } catch (AddressException e) {
                throw Throwables.propagate(e);
            }
        }
    };

    public static MimeMessage defaultMimeMessage() {
        return new MimeMessage(Session.getDefaultInstance(new Properties()));
    }

    public static MimeMessage mimeMessageFromStream(InputStream inputStream) throws MessagingException {
        return new MimeMessage(Session.getDefaultInstance(new Properties()), inputStream);
    }

    public static MimeMessageBuilder mimeMessageBuilder() {
        return new MimeMessageBuilder();
    }

    public static MultipartBuilder multipartBuilder() {
        return new MultipartBuilder();
    }

    public static BodyPartBuilder bodyPartBuilder() {
        return new BodyPartBuilder();
    }

    public static BodyPart bodyPartFromBytes(byte[] bytes) throws MessagingException {
        return new MimeBodyPart(new ByteArrayInputStream(bytes));
    }

    private Optional<String> text = Optional.absent();
    private Optional<String> subject = Optional.absent();
    private Optional<InternetAddress> sender = Optional.absent();
    private Optional<InternetAddress> from = Optional.absent();
    private Optional<MimeMultipart> content = Optional.absent();
    private ImmutableList.Builder<InternetAddress> cc = ImmutableList.builder();
    private ImmutableList.Builder<InternetAddress> to = ImmutableList.builder();
    private ImmutableList.Builder<InternetAddress> bcc = ImmutableList.builder();
    private ImmutableList.Builder<Header> headers = ImmutableList.builder();

    public MimeMessageBuilder setText(String text) {
        this.text = Optional.of(text);
        return this;
    }

    public MimeMessageBuilder addToRecipient(String text) throws AddressException {
        this.to.add(new InternetAddress(text));
        return this;
    }

    public MimeMessageBuilder setSubject(String subject) {
        this.subject = Optional.fromNullable(subject);
        return this;
    }

    public MimeMessageBuilder setSender(String sender) throws AddressException {
        this.sender = Optional.of(new InternetAddress(sender));
        return this;
    }

    public MimeMessageBuilder setFrom(String from) throws AddressException {
        this.from = Optional.of(new InternetAddress(from));
        return this;
    }

    public MimeMessageBuilder addCcRecipient(String text) throws AddressException {
        this.cc.add(new InternetAddress(text));
        return this;
    }

    public MimeMessageBuilder addBccRecipient(String text) throws AddressException {
        this.bcc.add(new InternetAddress(text));
        return this;
    }

    public MimeMessageBuilder addToRecipient(String... tos) throws AddressException {
        this.to.addAll(FluentIterable.from(Arrays.asList(tos))
            .transform(TO_INTERNET_ADDRESS)
            .toList());
        return this;
    }

    public MimeMessageBuilder addCcRecipient(String... ccs) throws AddressException {
        this.cc.addAll(FluentIterable.from(Arrays.asList(ccs))
            .transform(TO_INTERNET_ADDRESS)
            .toList());
        return this;
    }

    public MimeMessageBuilder addBccRecipient(String... bccs) throws AddressException {
        this.bcc.addAll(FluentIterable.from(Arrays.asList(bccs))
            .transform(TO_INTERNET_ADDRESS)
            .toList());
        return this;
    }

    public MimeMessageBuilder setContent(MimeMultipart mimeMultipart) {
        this.content = Optional.of(mimeMultipart);
        return this;
    }

    public MimeMessageBuilder setMultipartWithBodyParts(BodyPart... bobyParts) throws MessagingException {
        this.content = Optional.of(MimeMessageBuilder.multipartBuilder()
            .addBodies(bobyParts)
            .build());
        return this;
    }

    public MimeMessageBuilder setMultipartWithSubMessage(MimeMessage mimeMessage) throws MessagingException, IOException {
        return setMultipartWithBodyParts(
            new MimeBodyPart(
                new InternetHeaders(new ByteArrayInputStream("Content-Type: multipart/mixed".getBytes(Charsets.US_ASCII))),
                IOUtils.toByteArray(mimeMessage.getInputStream())));
    }

    public MimeMessageBuilder addHeader(String name, String value) {
        this.headers.add(new Header(name, value));
        return this;
    }

    public MimeMessageBuilder addHeaders(Header... headers) {
        this.headers.addAll(Arrays.asList(headers));
        return this;
    }

    public MimeMessage build() throws MessagingException {
        Preconditions.checkState(!(text.isPresent() && content.isPresent()), "Can not get at the same time a text and a content");
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        if (text.isPresent()) {
            BodyPart bodyPart = new MimeBodyPart();
            bodyPart.setText(text.get());
            mimeMessage.setContent(bodyPart, "text/plain");
        }
        if (content.isPresent()) {
            mimeMessage.setContent(content.get());
        }
        if (sender.isPresent()) {
            mimeMessage.setSender(sender.get());
        }
        if (from.isPresent()) {
            mimeMessage.setFrom(from.get());
        }
        if (subject.isPresent()) {
            mimeMessage.setSubject(subject.get());
        }
        List<InternetAddress> toAddresses = to.build();
        if (!toAddresses.isEmpty()) {
            mimeMessage.setRecipients(Message.RecipientType.TO, toAddresses.toArray(new InternetAddress[toAddresses.size()]));
        }
        List<InternetAddress> ccAddresses = cc.build();
        if (!ccAddresses.isEmpty()) {
            mimeMessage.setRecipients(Message.RecipientType.CC, ccAddresses.toArray(new InternetAddress[ccAddresses.size()]));
        }
        List<InternetAddress> bccAddresses = bcc.build();
        if (!bccAddresses.isEmpty()) {
            mimeMessage.setRecipients(Message.RecipientType.BCC, bccAddresses.toArray(new InternetAddress[bccAddresses.size()]));
        }
        List<Header> headerList = headers.build();
        for(Header header: headerList) {
            mimeMessage.addHeader(header.name, header.value);
        }
        mimeMessage.saveChanges();
        return mimeMessage;
    }

}
