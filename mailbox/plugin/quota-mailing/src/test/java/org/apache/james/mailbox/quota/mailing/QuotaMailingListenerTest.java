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

package org.apache.james.mailbox.quota.mailing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.apache.james.core.User;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.mock.MockMailboxSession;
import org.apache.james.mailbox.model.Quota;
import org.apache.james.mailbox.model.QuotaRoot;
import org.apache.james.mailbox.quota.QuotaCount;
import org.apache.james.mailbox.quota.QuotaSize;
import org.apache.james.mailbox.quota.QuotaThresholdChangesStore;
import org.apache.james.mailbox.quota.model.QuotaThreshold;
import org.apache.james.mailbox.quota.model.QuotaThresholdChange;
import org.apache.james.mailbox.quota.model.QuotaThresholdChanges;
import org.apache.james.mailbox.quota.model.QuotaThresholds;
import org.apache.james.user.memory.MemoryUsersRepository;
import org.apache.mailet.base.MailAddressFixture;
import org.apache.mailet.base.test.FakeMailContext;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

public interface QuotaMailingListenerTest {

    Duration GRACE_PERIOD = Duration.ofDays(1);
    QuotaThresholds SINGLE_THRESHOLD = new QuotaThresholds(ImmutableList.of(new QuotaThreshold(0.5)));
    String BOB = "bob@domain";
    User BOB_USER = User.fromUsername(BOB);

    @Test
    default void shouldNotSendMailWhenUnderAllThresholds(QuotaThresholdChangesStore store) {
        FakeMailContext mailetContext = mailetContext();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext, MemoryUsersRepository.withVirtualHosting(), store);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(30))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).isEmpty();
    }

    @Test
    default void shouldNotSendMailWhenNoThresholdUpdate(QuotaThresholdChangesStore store) {
        FakeMailContext mailetContext = mailetContext();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext, MemoryUsersRepository.withVirtualHosting(), store);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        store.persistQuotaSizeThresholdChange(BOB_USER,
            new QuotaThresholdChange(new QuotaThreshold(0.5),
                Instant.now().minus(Duration.ofDays(2))));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).isEmpty();
    }

    @Test
    default void shouldNotSendMailWhenThresholdOverPassedRecently(QuotaThresholdChangesStore store) {
        FakeMailContext mailetContext = mailetContext();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext, MemoryUsersRepository.withVirtualHosting(), store);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        store.persistQuotaSizeThresholdChange(BOB_USER,
            new QuotaThresholdChange(new QuotaThreshold(0.5),
                Instant.now().minus(Duration.ofHours(12))));
        store.persistQuotaSizeThresholdChange(BOB_USER,
            new QuotaThresholdChange(QuotaThreshold.ZERO,
                Instant.now().minus(Duration.ofHours(6))));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).isEmpty();
    }

    @Test
    default void shouldSendMailWhenThresholdOverPassed(QuotaThresholdChangesStore store) {
        FakeMailContext mailetContext = mailetContext();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext, MemoryUsersRepository.withVirtualHosting(), store);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).hasSize(1);
    }

    @Test
    default void shouldNotSendDuplicates(QuotaThresholdChangesStore store) {
        FakeMailContext mailetContext = mailetContext();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext, MemoryUsersRepository.withVirtualHosting(), store);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(60))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).hasSize(1);
    }

    @Test
    default void shouldNotifySeparatelyCountAndSize(QuotaThresholdChangesStore store) {
        FakeMailContext mailetContext = mailetContext();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext, MemoryUsersRepository.withVirtualHosting(), store);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(52))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(60))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).hasSize(2);
    }

    @Test
    default void shouldGroupSizeAndCountNotificationsWhenTriggeredByASingleEvent(QuotaThresholdChangesStore store) {
        FakeMailContext mailetContext = mailetContext();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext, MemoryUsersRepository.withVirtualHosting(), store);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(52))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).hasSize(1);
    }

    @Test
    default void shouldSendMailWhenThresholdOverPassedOverGracePeriod(QuotaThresholdChangesStore store) {
        FakeMailContext mailetContext = mailetContext();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext, MemoryUsersRepository.withVirtualHosting(), store);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        store.persistQuotaSizeThresholdChange(BOB_USER,
            new QuotaThresholdChange(new QuotaThreshold(0.5),
                Instant.now().minus(Duration.ofDays(12))));
        store.persistQuotaSizeThresholdChange(BOB_USER,
            new QuotaThresholdChange(QuotaThreshold.ZERO,
                Instant.now().minus(Duration.ofDays(6))));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).hasSize(1);
    }

    @Test
    default void shouldNotSendMailWhenNoThresholdUpdateForCount(QuotaThresholdChangesStore store) {
        FakeMailContext mailetContext = mailetContext();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext, MemoryUsersRepository.withVirtualHosting(), store);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        store.persistQuotaCountThresholdChange(BOB_USER,
            new QuotaThresholdChange(new QuotaThreshold(0.5),
                Instant.now().minus(Duration.ofDays(2))));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(55))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(40))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).isEmpty();
    }

    @Test
    default void shouldNotSendMailWhenThresholdOverPassedRecentlyForCount(QuotaThresholdChangesStore store) {
        FakeMailContext mailetContext = mailetContext();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext, MemoryUsersRepository.withVirtualHosting(), store);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        store.persistQuotaCountThresholdChange(BOB_USER,
            new QuotaThresholdChange(new QuotaThreshold(0.5),
                Instant.now().minus(Duration.ofHours(12))));
        store.persistQuotaCountThresholdChange(BOB_USER,
            new QuotaThresholdChange(QuotaThreshold.ZERO,
                Instant.now().minus(Duration.ofHours(6))));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(55))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(40))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).isEmpty();
    }

    @Test
    default void shouldSendMailWhenThresholdOverPassedForCount(QuotaThresholdChangesStore store) {
        FakeMailContext mailetContext = mailetContext();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext, MemoryUsersRepository.withVirtualHosting(), store);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(55))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(40))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).hasSize(1);
    }

    @Test
    default void shouldSendMailWhenThresholdOverPassedOverGracePeriodForCount(QuotaThresholdChangesStore store) {
        FakeMailContext mailetContext = mailetContext();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext, MemoryUsersRepository.withVirtualHosting(), store);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        store.persistQuotaCountThresholdChange(BOB_USER,
            new QuotaThresholdChange(new QuotaThreshold(0.5),
                Instant.now().minus(Duration.ofDays(12))));
        store.persistQuotaCountThresholdChange(BOB_USER,
            new QuotaThresholdChange(QuotaThreshold.ZERO,
                Instant.now().minus(Duration.ofDays(6))));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).hasSize(1);
    }

    default FakeMailContext mailetContext() {
        return FakeMailContext.builder()
            .postmaster(MailAddressFixture.POSTMASTER_AT_JAMES)
            .build();
    }

    @Test
    default void shouldUpdateSizeChangesWhenOverPassingLimit(QuotaThresholdChangesStore store) {
        FixedInstantSupplier instantSupplier = new FixedInstantSupplier();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext(), MemoryUsersRepository.withVirtualHosting(), store, instantSupplier);
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaSizeThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdChanges(
                new QuotaThresholdChange(
                    new QuotaThreshold(0.5),
                    instantSupplier.now())));
    }

    @Test
    default void shouldNotUpdateSizeChangesWhenNoChanges(QuotaThresholdChangesStore store) {
        FixedInstantSupplier instantSupplier = new FixedInstantSupplier();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext(), MemoryUsersRepository.withVirtualHosting(), store, instantSupplier);
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(30))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaSizeThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdChanges());
    }

    @Test
    default void shouldNotUpdateSizeChangesWhenNoChange(QuotaThresholdChangesStore store) {
        FixedInstantSupplier instantSupplier = new FixedInstantSupplier();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext(), MemoryUsersRepository.withVirtualHosting(), store, instantSupplier);
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));


        QuotaThresholdChange oldChange = new QuotaThresholdChange(new QuotaThreshold(0.5),
            instantSupplier.now().minus(Duration.ofDays(6)));
        store.persistQuotaSizeThresholdChange(BOB_USER, oldChange);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaSizeThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdChanges(oldChange));
    }

    @Test
    default void shouldUpdateSizeChangesWhenBelow(QuotaThresholdChangesStore store) {
        FixedInstantSupplier instantSupplier = new FixedInstantSupplier();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext(), MemoryUsersRepository.withVirtualHosting(), store, instantSupplier);
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));


        QuotaThresholdChange oldChange = new QuotaThresholdChange(new QuotaThreshold(0.5),
            instantSupplier.now().minus(Duration.ofDays(6)));
        store.persistQuotaSizeThresholdChange(BOB_USER, oldChange);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(30))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaSizeThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdChanges(oldChange,
                new QuotaThresholdChange(QuotaThreshold.ZERO, instantSupplier.now())));
    }

    @Test
    default void shouldUpdateSizeChangesWhenAbove(QuotaThresholdChangesStore store) {
        FixedInstantSupplier instantSupplier = new FixedInstantSupplier();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext(), MemoryUsersRepository.withVirtualHosting(), store, instantSupplier);
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));


        QuotaThresholdChange oldChange = new QuotaThresholdChange(QuotaThreshold.ZERO,
            instantSupplier.now().minus(Duration.ofDays(6)));
        store.persistQuotaSizeThresholdChange(BOB_USER, oldChange);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaSizeThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdChanges(oldChange,
                new QuotaThresholdChange(new QuotaThreshold(0.5), instantSupplier.now())));
    }

    @Test
    default void shouldUpdateSizeChangesWhenAboveButRecentlyOverpasses(QuotaThresholdChangesStore store) {
        FixedInstantSupplier instantSupplier = new FixedInstantSupplier();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext(), MemoryUsersRepository.withVirtualHosting(), store, instantSupplier);
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));


        QuotaThresholdChange oldChange1 = new QuotaThresholdChange(new QuotaThreshold(0.5),
            instantSupplier.now().minus(Duration.ofHours(12)));
        QuotaThresholdChange oldChange2 = new QuotaThresholdChange(QuotaThreshold.ZERO,
            instantSupplier.now().minus(Duration.ofHours(6)));
        store.persistQuotaSizeThresholdChange(BOB_USER, oldChange1);
        store.persistQuotaSizeThresholdChange(BOB_USER, oldChange2);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaSizeThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdChanges(oldChange1, oldChange2,
                new QuotaThresholdChange(new QuotaThreshold(0.5), instantSupplier.now())));
    }

    @Test
    default void shouldUpdateCountChangesWhenOverPassingLimit(QuotaThresholdChangesStore store) {
        FixedInstantSupplier instantSupplier = new FixedInstantSupplier();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext(), MemoryUsersRepository.withVirtualHosting(), store, instantSupplier);
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(55))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(40))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaCountThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdChanges(
                new QuotaThresholdChange(
                    new QuotaThreshold(0.5),
                    instantSupplier.now())));
    }

    @Test
    default void shouldNotUpdateCountChangesWhenNoChanges(QuotaThresholdChangesStore store) {
        FixedInstantSupplier instantSupplier = new FixedInstantSupplier();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext(), MemoryUsersRepository.withVirtualHosting(), store, instantSupplier);
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(30))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaCountThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdChanges());
    }

    @Test
    default void shouldNotUpdateCountChangesWhenNoChange(QuotaThresholdChangesStore store) {
        FixedInstantSupplier instantSupplier = new FixedInstantSupplier();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext(), MemoryUsersRepository.withVirtualHosting(), store, instantSupplier);
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));


        QuotaThresholdChange oldChange = new QuotaThresholdChange(new QuotaThreshold(0.5),
            instantSupplier.now().minus(Duration.ofDays(6)));
        store.persistQuotaCountThresholdChange(BOB_USER, oldChange);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(55))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(40))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaCountThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdChanges(oldChange));
    }

    @Test
    default void shouldUpdateCountChangesWhenBelow(QuotaThresholdChangesStore store) {
        FixedInstantSupplier instantSupplier = new FixedInstantSupplier();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext(), MemoryUsersRepository.withVirtualHosting(), store, instantSupplier);
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));


        QuotaThresholdChange oldChange = new QuotaThresholdChange(new QuotaThreshold(0.5),
            instantSupplier.now().minus(Duration.ofDays(6)));
        store.persistQuotaCountThresholdChange(BOB_USER, oldChange);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(30))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaCountThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdChanges(oldChange,
                new QuotaThresholdChange(QuotaThreshold.ZERO, instantSupplier.now())));
    }

    @Test
    default void shouldUpdateCountChangesWhenAbove(QuotaThresholdChangesStore store) {
        FixedInstantSupplier instantSupplier = new FixedInstantSupplier();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext(), MemoryUsersRepository.withVirtualHosting(), store, instantSupplier);
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));


        QuotaThresholdChange oldChange = new QuotaThresholdChange(QuotaThreshold.ZERO,
            instantSupplier.now().minus(Duration.ofDays(6)));
        store.persistQuotaCountThresholdChange(BOB_USER, oldChange);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(55))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(40))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaCountThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdChanges(oldChange,
                new QuotaThresholdChange(new QuotaThreshold(0.5), instantSupplier.now())));
    }

    @Test
    default void shouldUpdateCountChangesWhenAboveButRecentlyOverpasses(QuotaThresholdChangesStore store) {
        FixedInstantSupplier instantSupplier = new FixedInstantSupplier();
        QuotaMailingListener testee = new QuotaMailingListener(mailetContext(), MemoryUsersRepository.withVirtualHosting(), store, instantSupplier);
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));


        QuotaThresholdChange oldChange1 = new QuotaThresholdChange(new QuotaThreshold(0.5),
            instantSupplier.now().minus(Duration.ofHours(12)));
        QuotaThresholdChange oldChange2 = new QuotaThresholdChange(QuotaThreshold.ZERO,
            instantSupplier.now().minus(Duration.ofHours(6)));
        store.persistQuotaCountThresholdChange(BOB_USER, oldChange1);
        store.persistQuotaCountThresholdChange(BOB_USER, oldChange2);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(55))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(40))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaCountThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdChanges(oldChange1, oldChange2,
                new QuotaThresholdChange(new QuotaThreshold(0.5), instantSupplier.now())));
    }
}