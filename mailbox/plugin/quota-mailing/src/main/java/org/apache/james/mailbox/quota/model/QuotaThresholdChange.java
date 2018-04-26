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

package org.apache.james.mailbox.quota.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import com.google.common.base.MoreObjects;

public class QuotaThresholdChange {
    private final QuotaThreshold quotaThreshold;
    private final Instant date;

    public QuotaThresholdChange(QuotaThreshold quotaThreshold, Instant date) {
        this.quotaThreshold = quotaThreshold;
        this.date = date;
    }

    public boolean isNotOlderThan(Duration duration) {
        return date.isAfter(Instant.now().minus(duration));
    }

    public QuotaThreshold getQuotaThreshold() {
        return quotaThreshold;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof QuotaThresholdChange) {
            QuotaThresholdChange that = (QuotaThresholdChange) o;

            return Objects.equals(this.quotaThreshold, that.quotaThreshold)
                && Objects.equals(this.date, that.date);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(quotaThreshold, date);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("quotaThreshold", quotaThreshold)
            .add("date", date)
            .toString();
    }
}
