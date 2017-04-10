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

package org.apache.mailet;
import java.util.Collection;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;

public class PerRecipientHeaders {
    public static final Function<Header, String> GET_HEADER_NAME = new Function<Header, String>() {
        @Override
        public String apply(Header input) {
            return input.getName();
        }
    };

    private Multimap<MailAddress, Header> headersByRecipient;

    public PerRecipientHeaders() {
        headersByRecipient = ArrayListMultimap.create();
    }

    public Collection<MailAddress> getRecipientsWithSpecificHeaders() {
        return headersByRecipient.keySet();
    }

    public Collection<Header> getHeadersForRecipient(MailAddress recipient) {
        return headersByRecipient.get(recipient);
    }

    public Collection<String> getHeaderNamesForRecipient(MailAddress recipient) {
        return FluentIterable.from(headersByRecipient.get(recipient))
            .transform(GET_HEADER_NAME)
            .toSet();
    }

    public void addHeaderForRecipient(Header header, MailAddress recipient) {
        headersByRecipient.put(recipient, header);
    }

    public static class Header {
        private final String name;
        private final String value;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String name;
            private String value;
            
            public Builder name(String name) {
                this.name = name;
                return this;
            }
            
            public Builder value(String value) {
                this.value = value;
                return this;
            }
            
            public Header build() {
                Preconditions.checkNotNull(name);
                Preconditions.checkNotNull(value);
                return new Header(name, value);
            }
        }

        @VisibleForTesting
        Header(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        @Override
        public final boolean equals(Object o) {
            if (o instanceof Header) {
                Header that = (Header) o;

                return Objects.equal(this.name, that.name)
                    && Objects.equal(this.value, that.value);
            }
            return false;
        }

        @Override
        public final int hashCode() {
            return Objects.hashCode(name, value);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("value", value)
                .toString();
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof PerRecipientHeaders) {
            PerRecipientHeaders that = (PerRecipientHeaders) o;

            return Objects.equal(this.headersByRecipient, that.headersByRecipient);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(headersByRecipient);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("headersByRecipient", headersByRecipient)
            .toString();
    }
}
