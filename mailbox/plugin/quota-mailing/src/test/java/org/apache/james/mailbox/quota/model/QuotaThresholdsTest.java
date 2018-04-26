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

import org.apache.james.mailbox.model.Quota;
import org.apache.james.mailbox.quota.QuotaSize;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

public class QuotaThresholdsTest {

    @Test
    public void firstExceededThresholdShouldReturnZeroWhenBelowAllThresholds() {
        assertThat(
            new QuotaThresholds(
                ImmutableList.of(
                    new QuotaThreshold(0.5),
                    new QuotaThreshold(0.8),
                    new QuotaThreshold(0.95),
                    new QuotaThreshold(0.99)))
                .firstExceededThreshold(Quota.<QuotaSize>builder()
                    .used(QuotaSize.size(40))
                    .computedLimit(QuotaSize.size(100))
                    .build()))
            .isEqualTo(QuotaThreshold.ZERO);
    }

    @Test
    public void firstExceededThresholdShouldReturnHighestExceededThreshold() {
        assertThat(
            new QuotaThresholds(
                ImmutableList.of(
                    new QuotaThreshold(0.5),
                    new QuotaThreshold(0.8),
                    new QuotaThreshold(0.95),
                    new QuotaThreshold(0.99)))
                .firstExceededThreshold(Quota.<QuotaSize>builder()
                    .used(QuotaSize.size(92))
                    .computedLimit(QuotaSize.size(100))
                    .build()))
            .isEqualTo(new QuotaThreshold(0.8));
    }

    @Test
    public void firstExceededThresholdShouldReturnHighestThresholdWhenAboveAllThresholds() {
        assertThat(
            new QuotaThresholds(
                ImmutableList.of(
                    new QuotaThreshold(0.5),
                    new QuotaThreshold(0.8),
                    new QuotaThreshold(0.95),
                    new QuotaThreshold(0.99)))
                .firstExceededThreshold(Quota.<QuotaSize>builder()
                    .used(QuotaSize.size(992))
                    .computedLimit(QuotaSize.size(1000))
                    .build()))
            .isEqualTo(new QuotaThreshold(0.99));
    }

    @Test
    public void firstExceededThresholdShouldReturnZeroWhenNoThresholds() {
        assertThat(
            new QuotaThresholds(
                ImmutableList.of())
                .firstExceededThreshold(Quota.<QuotaSize>builder()
                    .used(QuotaSize.size(992))
                    .computedLimit(QuotaSize.size(1000))
                    .build()))
            .isEqualTo(QuotaThreshold.ZERO);
    }

}