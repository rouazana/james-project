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
package org.apache.james.quota.search.elasticsearch.json;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class QuotaRatioAsJson {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String user;
        private Optional<String> domain;
        private Double quotaCountRatio;
        private Double quotaSizeRatio;

        private Builder() {
            domain = Optional.empty();
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder domain(Optional<String> domain) {
            this.domain = domain;
            return this;
        }

        public Builder quotaCountRatio(double quotaCountRatio) {
            this.quotaCountRatio = quotaCountRatio;
            return this;
        }

        public Builder quotaSizeRatio(double quotaSizeRatio) {
            this.quotaSizeRatio = quotaSizeRatio;
            return this;
        }

        public QuotaRatioAsJson build() {
            Preconditions.checkState(!Strings.isNullOrEmpty(user), "'user' is mandatory");
            Preconditions.checkNotNull(quotaCountRatio, "'quotaCountRatio' is mandatory");
            Preconditions.checkNotNull(quotaSizeRatio, "'quotaSizeRatio' is mandatory");

            return new QuotaRatioAsJson(user, domain, quotaCountRatio, quotaSizeRatio);
        }
    }

    private final String user;
    private final Optional<String> domain;
    private final double quotaCountRatio;
    private final double quotaSizeRatio;

    private QuotaRatioAsJson(String user, Optional<String> domain, double quotaCountRatio, double quotaSizeRatio) {
        this.user = user;
        this.domain = domain;
        this.quotaCountRatio = quotaCountRatio;
        this.quotaSizeRatio = quotaSizeRatio;
    }

    @JsonProperty(JsonMessageConstants.USER)
    public String getUser() {
        return user;
    }

    @JsonProperty(JsonMessageConstants.DOMAIN)
    public Optional<String> getDomain() {
        return domain;
    }

    @JsonProperty(JsonMessageConstants.QUOTA_COUNT_RATIO)
    public double getQuotaCountRatio() {
        return quotaCountRatio;
    }

    @JsonProperty(JsonMessageConstants.QUOTA_SIZE_RATIO)
    public double getQuotaSizeRatio() {
        return quotaSizeRatio;
    }
}
