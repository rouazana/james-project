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

import java.time.Instant;
import java.util.Optional;

import javax.inject.Inject;
import javax.mail.MessagingException;

import org.apache.james.core.MailAddress;
import org.apache.james.core.User;
import org.apache.james.core.builder.MimeMessageBuilder;
import org.apache.james.mailbox.Event;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.quota.CompareWithCurrentThreshold;
import org.apache.james.mailbox.quota.QuotaThresholdHistoryStore;
import org.apache.james.mailbox.quota.model.QuotaThreshold;
import org.apache.james.mailbox.quota.model.QuotaThresholdChange;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.util.FunctionalInterfacesUtils.Action;
import org.apache.mailet.MailetContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class QuotaMailingListener implements MailboxListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuotaMailingListener.class);
    private final MailetContext mailetContext;
    private final UsersRepository usersRepository;
    private final QuotaThresholdHistoryStore quotaThresholdHistoryStore;
    private final InstantSupplier instantSupplier;
    private QuotaMailingListenerConfiguration configuration;

    @Inject
    public QuotaMailingListener(MailetContext mailetContext, UsersRepository usersRepository, QuotaThresholdHistoryStore quotaThresholdHistoryStore) {
        this(mailetContext, usersRepository, quotaThresholdHistoryStore, Instant::now);
    }

    public QuotaMailingListener(MailetContext mailetContext, UsersRepository usersRepository, QuotaThresholdHistoryStore quotaThresholdHistoryStore, InstantSupplier instantSupplier) {
        this.mailetContext = mailetContext;
        this.usersRepository = usersRepository;
        this.quotaThresholdHistoryStore = quotaThresholdHistoryStore;
        this.configuration = QuotaMailingListenerConfiguration.DEFAULT;
        this.instantSupplier = instantSupplier;
    }

    public void configure(QuotaMailingListenerConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public ListenerType getType() {
        return ListenerType.ONCE;
    }

    @Override
    public ExecutionMode getExecutionMode() {
        return ExecutionMode.SYNCHRONOUS;
    }

    @Override
    public void event(Event event) {
        try {
            if (event instanceof QuotaUsageUpdatedEvent) {
                QuotaUsageUpdatedEvent updatedEvent = (QuotaUsageUpdatedEvent) event;
                User user = getUser(event);
                handleEvent(updatedEvent, user);
            }
        } catch (Exception e) {
            LOGGER.error("Can not send a quota mail alert", e);
        }
    }

    public void handleEvent(QuotaUsageUpdatedEvent updatedEvent, User user) throws UsersRepositoryException, MessagingException {
        Optional<InformationToEmail> notice = computeInformationToEmail(user, updatedEvent);

        if (notice.isPresent()) {
            MailAddress sender = mailetContext.getPostmaster();
            MailAddress recipient = usersRepository.getMailAddressFor(user);

            mailetContext.sendMail(sender, ImmutableList.of(recipient),
                MimeMessageBuilder.mimeMessageBuilder()
                    .addFrom(sender.asString())
                    .addToRecipient(recipient.asString())
                    .setSubject("Warning: Your email usage just exceeded a configured threshold")
                    .setText(notice.get().generateReport())
                    .build());
        }
    }

    public Optional<InformationToEmail> computeInformationToEmail(User user, QuotaUsageUpdatedEvent event) {
        QuotaThreshold countThreshold = configuration.getThresholds().highestExceededThreshold(event.getCountQuota());
        CompareWithCurrentThreshold compareWithCountThreshold = quotaThresholdHistoryStore
            .retrieveQuotaCountThresholdChanges(user)
            .compareWithCurrentThreshold(countThreshold, configuration.getGracePeriod());

        QuotaThreshold sizeThreshold = configuration.getThresholds().highestExceededThreshold(event.getSizeQuota());
        CompareWithCurrentThreshold compareWithCurrentSizeThreshold = quotaThresholdHistoryStore
            .retrieveQuotaSizeThresholdChanges(user)
            .compareWithCurrentThreshold(sizeThreshold, configuration.getGracePeriod());
        if (compareWithCountThreshold != CompareWithCurrentThreshold.NO_CHANGES) {
            Action updateCountThreshold = () -> quotaThresholdHistoryStore.persistQuotaCountThresholdChange(user, new QuotaThresholdChange(countThreshold, instantSupplier.now()));
            updateThreshold(compareWithCountThreshold, updateCountThreshold);
        }
        if (compareWithCurrentSizeThreshold != CompareWithCurrentThreshold.NO_CHANGES) {
            Action updateSizeThreshold = () -> quotaThresholdHistoryStore.persistQuotaSizeThresholdChange(user, new QuotaThresholdChange(sizeThreshold, instantSupplier.now()));
            updateThreshold(compareWithCurrentSizeThreshold, updateSizeThreshold);
        }

        return InformationToEmail.builder()
            .countQuota(event.getCountQuota())
            .sizeQuota(event.getSizeQuota())
            .countThreshold(countThreshold, compareWithCountThreshold)
            .sizeThreshold(sizeThreshold, compareWithCurrentSizeThreshold)
            .build();
    }

    private void updateThreshold(CompareWithCurrentThreshold comparison, Action updateThreshold) {
        switch (comparison) {
            case ABOVE_CURRENT_THRESHOLD:
            case ABOVE_CURRENT_THRESHOLD_WITH_RECENT_CHANGES:
            case BELOW_CURRENT_THRESHOLD:
                updateThreshold.perform();
        }
    }

    private User getUser(Event event) {
        return User.fromUsername(
            event.getSession()
            .getUser()
            .getUserName());
    }
}
