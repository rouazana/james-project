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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.apache.james.mailbox.quota.CompareWithCurrentThreshold;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

import nl.jqno.equalsverifier.EqualsVerifier;

public class QuotaThresholdChangesTest {

    @Test
    public void shouldMatchBeanContract() {
        EqualsVerifier.forClass(QuotaThresholdChanges.class)
            .allFieldsShouldBeUsed()
            .verify();
    }

    @Test
    public void compareWithCurrentThresholdShouldReturnAboveWhenStrictlyAboveDuringDuration() {
        assertThat(
            new QuotaThresholdChanges(
                ImmutableList.of(
                new QuotaThresholdChange(
                    new QuotaThreshold(0.5),
                    Instant.now().minus(Duration.ofDays(24))),
                new QuotaThresholdChange(
                    new QuotaThreshold(0.75),
                    Instant.now().minus(Duration.ofDays(12))),
                new QuotaThresholdChange(
                    new QuotaThreshold(0.5),
                    Instant.now().minus(Duration.ofDays(6)))))
                .compareWithCurrentThreshold(
                    new QuotaThreshold(0.75),
                    Duration.ofDays(1)))
            .isEqualTo(CompareWithCurrentThreshold.ABOVE_CURRENT_THRESHOLD);
    }

    @Test
    public void compareWithCurrentThresholdShouldReturnBelowWhenLowerThanLastChange() {
        assertThat(
            new QuotaThresholdChanges(
                ImmutableList.of(
                new QuotaThresholdChange(
                    new QuotaThreshold(0.5),
                    Instant.now().minus(Duration.ofDays(24))),
                new QuotaThresholdChange(
                    new QuotaThreshold(0.75),
                    Instant.now().minus(Duration.ofDays(12)))))
                .compareWithCurrentThreshold(
                    new QuotaThreshold(0.5),
                    Duration.ofDays(1)))
            .isEqualTo(CompareWithCurrentThreshold.BELOW_CURRENT_THRESHOLD);
    }

    @Test
    public void compareWithCurrentThresholdShouldReturnNoChangeWhenEqualsLastChange() {
        assertThat(
            new QuotaThresholdChanges(
                ImmutableList.of(
                new QuotaThresholdChange(
                    new QuotaThreshold(0.5),
                    Instant.now().minus(Duration.ofDays(24))),
                new QuotaThresholdChange(
                    new QuotaThreshold(0.75),
                    Instant.now().minus(Duration.ofDays(12)))))
                .compareWithCurrentThreshold(
                    new QuotaThreshold(0.75),
                    Duration.ofDays(1)))
            .isEqualTo(CompareWithCurrentThreshold.NO_CHANGES);
    }

    @Test
    public void compareWithCurrentThresholdShouldReturnAboveWithRecentChangesWhenThresholdExceededDuringDuration() {
        assertThat(
            new QuotaThresholdChanges(
                ImmutableList.of(
                    new QuotaThresholdChange(
                        new QuotaThreshold(0.5),
                        Instant.now().minus(Duration.ofDays(24))),
                    new QuotaThresholdChange(
                        new QuotaThreshold(0.75),
                        Instant.now().minus(Duration.ofHours(12))),
                    new QuotaThresholdChange(
                        new QuotaThreshold(0.5),
                        Instant.now().minus(Duration.ofHours(6)))))
                .compareWithCurrentThreshold(
                    new QuotaThreshold(0.75),
                    Duration.ofDays(1)))
            .isEqualTo(CompareWithCurrentThreshold.ABOVE_CURRENT_THRESHOLD_WITH_RECENT_CHANGES);
    }
}