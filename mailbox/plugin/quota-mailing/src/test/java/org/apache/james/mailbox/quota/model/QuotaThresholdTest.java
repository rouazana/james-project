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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.james.mailbox.model.Quota;
import org.apache.james.mailbox.quota.QuotaSize;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class QuotaThresholdTest {

    @Test
    public void shouldMatchBeanContract() {
        EqualsVerifier.forClass(QuotaThreshold.class)
            .allFieldsShouldBeUsed()
            .verify();
    }

    @Test
    public void constructorShouldThrowBelowLowerValue() {
        assertThatThrownBy(() -> new QuotaThreshold(-0.00001))
            .isInstanceOf(IllegalArgumentException.class);
    }


    @Test
    public void constructorShouldThrowAboveUpperValue() {
        assertThatThrownBy(() -> new QuotaThreshold(1.00001))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void constructorShouldNotThrowOnLowerValue() {
        assertThatCode(() -> new QuotaThreshold(0.))
            .doesNotThrowAnyException();
    }

    @Test
    public void constructorShouldNotThrowOnUpperValue() {
        assertThatCode(() -> new QuotaThreshold(1.))
            .doesNotThrowAnyException();
    }

    @Test
    public void isExceededShouldReturnFalseWhenBelowThreshold() {
        QuotaThreshold quotaThreshold = new QuotaThreshold(0.75);

        Quota<QuotaSize> quota = Quota.<QuotaSize>builder()
            .computedLimit(QuotaSize.size(100))
            .used(QuotaSize.size(50))
            .build();

        assertThat(quotaThreshold.isExceeded(quota))
            .isFalse();
    }

    @Test
    public void isExceededShouldReturnTrueWhenAboveThreshold() {
        QuotaThreshold quotaThreshold = new QuotaThreshold(0.75);

        Quota<QuotaSize> quota = Quota.<QuotaSize>builder()
            .computedLimit(QuotaSize.size(100))
            .used(QuotaSize.size(80))
            .build();

        assertThat(quotaThreshold.isExceeded(quota))
            .isTrue();
    }

    @Test
    public void isExceededShouldReturnFalseWhenOnThreshold() {
        QuotaThreshold quotaThreshold = new QuotaThreshold(0.75);

        Quota<QuotaSize> quota = Quota.<QuotaSize>builder()
            .computedLimit(QuotaSize.size(100))
            .used(QuotaSize.size(75))
            .build();

        assertThat(quotaThreshold.isExceeded(quota))
            .isFalse();
    }

    @Test
    public void isExceededShouldReturnFalseWhenUnlimited() {
        QuotaThreshold quotaThreshold = new QuotaThreshold(0.75);

        Quota<QuotaSize> quota = Quota.<QuotaSize>builder()
            .computedLimit(QuotaSize.unlimited())
            .used(QuotaSize.size(80))
            .build();

        assertThat(quotaThreshold.isExceeded(quota))
            .isFalse();
    }

    @Test
    public void nonZeroShouldFilterZero() {
        assertThat(QuotaThreshold.ZERO.nonZero())
            .isEmpty();
    }

    @Test
    public void nonZeroShouldNotFilterNonZeroValues() {
        QuotaThreshold quotaThreshold = new QuotaThreshold(0.75);

        assertThat(quotaThreshold.nonZero())
            .contains(quotaThreshold);
    }

    @Test
    public void getQuotaOccupationRatioAsPercentShouldReturnIntRepresentationOfThreshold() {
        QuotaThreshold quotaThreshold = new QuotaThreshold(0.75);

        assertThat(quotaThreshold.getQuotaOccupationRatioAsPercent())
            .isEqualTo(75);
    }

    @Test
    public void getQuotaOccupationRatioAsPercentShouldTruncateValues() {
        QuotaThreshold quotaThreshold = new QuotaThreshold(0.759);

        assertThat(quotaThreshold.getQuotaOccupationRatioAsPercent())
            .isEqualTo(75);
    }

    @Test
    public void compareToShouldReturnNegativeWhenLowerThanComparedValue() {
        QuotaThreshold quotaThreshold1 = new QuotaThreshold(0.75);
        QuotaThreshold quotaThreshold2 = new QuotaThreshold(0.9);

        assertThat(quotaThreshold1.compareTo(quotaThreshold2))
            .isLessThan(0);
    }

    @Test
    public void compareToShouldReturnPositiveWhenHgherThanComparedValue() {
        QuotaThreshold quotaThreshold1 = new QuotaThreshold(0.75);
        QuotaThreshold quotaThreshold2 = new QuotaThreshold(0.9);

        assertThat(quotaThreshold2.compareTo(quotaThreshold1))
            .isGreaterThan(0);
    }

    @Test
    public void compareToShouldReturnZeroWhenEquals() {
        QuotaThreshold quotaThreshold = new QuotaThreshold(0.75);

        assertThat(quotaThreshold.compareTo(quotaThreshold))
            .isEqualTo(0);
    }

}