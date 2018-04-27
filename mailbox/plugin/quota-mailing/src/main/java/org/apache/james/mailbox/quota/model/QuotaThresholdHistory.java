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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.james.mailbox.quota.CompareWithCurrentThreshold;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class QuotaThresholdHistory {

    private final ImmutableList<QuotaThresholdChange> changes;

    public QuotaThresholdHistory() {
        this(ImmutableList.of());
    }

    public QuotaThresholdHistory(QuotaThresholdChange... changes) {
        this.changes = ImmutableList.copyOf(Arrays.asList(changes));
    }

    public QuotaThresholdHistory(List<QuotaThresholdChange> changes) {
        this.changes = ImmutableList.copyOf(changes);
    }

    public CompareWithCurrentThreshold compareWithCurrentThreshold(QuotaThreshold currentThreshold, Duration gracePeriod) {
        Optional<QuotaThreshold> lastThreshold = Optional.ofNullable(Iterables.getLast(changes, null))
            .map(QuotaThresholdChange::getQuotaThreshold);

        return compareWithCurrentThreshold(currentThreshold, gracePeriod, lastThreshold.orElse(QuotaThreshold.ZERO));
    }

    private CompareWithCurrentThreshold compareWithCurrentThreshold(QuotaThreshold currentThreshold, Duration gracePeriod, QuotaThreshold lastThreshold) {
        int comparisonResult = currentThreshold.compareTo(lastThreshold);

        if (comparisonResult < 0) {
            return CompareWithCurrentThreshold.BELOW_CURRENT_THRESHOLD;
        }
        if (comparisonResult == 0) {
            return CompareWithCurrentThreshold.NO_CHANGES;
        }
        return recentlyExceededQuotaThreshold(currentThreshold, gracePeriod)
                .map(any -> CompareWithCurrentThreshold.ABOVE_CURRENT_THRESHOLD_WITH_RECENT_CHANGES)
                .orElse(CompareWithCurrentThreshold.ABOVE_CURRENT_THRESHOLD);
    }

    private Optional<QuotaThresholdChange> recentlyExceededQuotaThreshold(QuotaThreshold currentThreshold, Duration gracePeriod) {
        return changes.stream()
            .filter(change -> change.isNotOlderThan(gracePeriod))
            .filter(change -> change.getQuotaThreshold().compareTo(currentThreshold) >= 0)
            .findFirst();
    }

    public QuotaThresholdHistory combineWith(QuotaThresholdChange change) {
        return new QuotaThresholdHistory(
            ImmutableList.<QuotaThresholdChange>builder()
                .addAll(changes)
                .add(change)
                .build());
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof QuotaThresholdHistory) {
            QuotaThresholdHistory that = (QuotaThresholdHistory) o;

            return Objects.equals(this.changes, that.changes);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(changes);
    }
}
