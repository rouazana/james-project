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

package org.apache.james.mailbox.store.quota;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.apache.james.mailbox.model.Quota;
import org.apache.james.mailbox.model.QuotaRoot;
import org.apache.james.mailbox.quota.CurrentQuotaManager;
import org.apache.james.mailbox.quota.MaxQuotaManager;
import org.apache.james.mailbox.quota.QuotaCount;
import org.apache.james.mailbox.quota.QuotaSize;
import org.junit.Before;
import org.junit.Test;

public class StoreQuotaManagerTest {

    private StoreQuotaManager testee;
    private CurrentQuotaManager mockedCurrentQuotaManager;
    private MaxQuotaManager mockedMaxQuotaManager;
    private QuotaRoot quotaRoot;

    @Before
    public void setUp() {
        mockedCurrentQuotaManager = mock(CurrentQuotaManager.class);
        mockedMaxQuotaManager = mock(MaxQuotaManager.class);
        testee = new StoreQuotaManager(mockedCurrentQuotaManager, mockedMaxQuotaManager);
        quotaRoot = QuotaRoot.quotaRoot("benwa");
    }

    @Test
    public void getMessageQuotaShouldWorkWithNumericValues() throws Exception {
        when(mockedMaxQuotaManager.getMaxMessage(quotaRoot)).thenReturn(Optional.of(QuotaCount.count(360L)));
        when(mockedCurrentQuotaManager.getCurrentMessageCount(quotaRoot)).thenReturn(QuotaCount.count(36L));
        assertThat(testee.getMessageQuota(quotaRoot)).isEqualTo(Quota.quota(QuotaCount.count(36L), QuotaCount.count(360L)));
    }

    @Test
    public void getStorageQuotaShouldWorkWithNumericValues() throws Exception {
        when(mockedMaxQuotaManager.getMaxStorage(quotaRoot)).thenReturn(Optional.of(QuotaSize.size(360L)));
        when(mockedCurrentQuotaManager.getCurrentStorage(quotaRoot)).thenReturn(QuotaSize.size(36L));
        assertThat(testee.getStorageQuota(quotaRoot)).isEqualTo(Quota.quota(QuotaSize.size(36L), QuotaSize.size(360L)));
    }

    @Test
    public void getStorageQuotaShouldNotCalculateCurrentQuotaWhenUnlimited() throws Exception {
        testee.setCalculateWhenUnlimited(false);
        when(mockedMaxQuotaManager.getMaxStorage(quotaRoot)).thenReturn(Optional.of(QuotaSize.unlimited()));

        assertThat(testee.getStorageQuota(quotaRoot)).isEqualTo(Quota.unknownUsedQuota(QuotaSize.unlimited()));
        verify(mockedCurrentQuotaManager, never()).getCurrentStorage(quotaRoot);
    }

    @Test
    public void getMessageQuotaShouldNotCalculateCurrentQuotaWhenUnlimited() throws Exception {
        testee.setCalculateWhenUnlimited(false);
        when(mockedMaxQuotaManager.getMaxMessage(quotaRoot)).thenReturn(Optional.of(QuotaCount.unlimited()));

        assertThat(testee.getMessageQuota(quotaRoot)).isEqualTo(Quota.unknownUsedQuota(QuotaCount.unlimited()));
        verify(mockedCurrentQuotaManager, never()).getCurrentMessageCount(quotaRoot);
    }

    @Test
    public void getStorageQuotaShouldCalculateCurrentQuotaWhenUnlimited() throws Exception {
        testee.setCalculateWhenUnlimited(true);
        when(mockedMaxQuotaManager.getMaxStorage(quotaRoot)).thenReturn(Optional.of(QuotaSize.unlimited()));
        when(mockedCurrentQuotaManager.getCurrentStorage(quotaRoot)).thenReturn(QuotaSize.size(36L));

        assertThat(testee.getStorageQuota(quotaRoot)).isEqualTo(Quota.quota(QuotaSize.size(36L), QuotaSize.unlimited()));
    }

    @Test
    public void getMessageQuotaShouldCalculateCurrentQuotaWhenUnlimited() throws Exception {
        testee.setCalculateWhenUnlimited(true);
        when(mockedMaxQuotaManager.getMaxMessage(quotaRoot)).thenReturn(Optional.of(QuotaCount.unlimited()));
        when(mockedCurrentQuotaManager.getCurrentMessageCount(quotaRoot)).thenReturn(QuotaCount.count(36L));

        assertThat(testee.getMessageQuota(quotaRoot)).isEqualTo(Quota.quota(QuotaCount.count(36L), QuotaCount.unlimited()));
    }

}
