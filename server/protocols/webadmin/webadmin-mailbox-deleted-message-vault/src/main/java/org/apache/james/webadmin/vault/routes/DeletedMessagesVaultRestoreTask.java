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

package org.apache.james.webadmin.vault.routes;

import static org.apache.james.webadmin.vault.routes.RestoreService.RestoreResult.RESTORE_SUCCEED;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.james.core.User;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.task.Task;
import org.apache.james.task.TaskExecutionDetails;
import org.apache.james.task.TaskType;
import org.apache.james.vault.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

public class DeletedMessagesVaultRestoreTask implements Task {

    static final TaskType TYPE = TaskType.of("deletedMessages/restore");

    public static class AdditionalInformation implements TaskExecutionDetails.AdditionalInformation {
        private final User user;
        private final long successfulRestoreCount;
        private final long errorRestoreCount;

        AdditionalInformation(User user, long successfulRestoreCount, long errorRestoreCount) {
            this.user = user;
            this.successfulRestoreCount = successfulRestoreCount;
            this.errorRestoreCount = errorRestoreCount;
        }

        public long getSuccessfulRestoreCount() {
            return successfulRestoreCount;
        }

        public long getErrorRestoreCount() {
            return errorRestoreCount;
        }

        public String getUser() {
            return user.asString();
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DeletedMessagesVaultRestoreTask.class);

    private final User userToRestore;
    private final RestoreService vaultRestore;
    private final AtomicLong successfulRestoreCount;
    private final AtomicLong errorRestoreCount;
    @VisibleForTesting
    final Query query;

    DeletedMessagesVaultRestoreTask(RestoreService vaultRestore, User userToRestore, Query query) {
        this.query = query;
        this.userToRestore = userToRestore;
        this.vaultRestore = vaultRestore;
        this.successfulRestoreCount = new AtomicLong();
        this.errorRestoreCount = new AtomicLong();
    }

    @Override
    public Result run() {
        try {
            return vaultRestore.restore(userToRestore, query).toStream()
                .peek(this::updateInformation)
                .map(this::restoreResultToTaskResult)
                .reduce(Task::combine)
                .orElse(Result.COMPLETED);
        } catch (MailboxException e) {
            LOGGER.error("Error happens while restoring user {}", userToRestore.asString(), e);
            return Result.PARTIAL;
        }
    }

    private Task.Result restoreResultToTaskResult(RestoreService.RestoreResult restoreResult) {
        if (restoreResult.equals(RESTORE_SUCCEED)) {
            return Result.COMPLETED;
        }
        return Result.PARTIAL;
    }

    private void updateInformation(RestoreService.RestoreResult restoreResult) {
        switch (restoreResult) {
            case RESTORE_FAILED:
                incrementErrorRestoreCount();
                break;
            case RESTORE_SUCCEED:
                incrementSuccessfulRestoreCount();
                break;
        }
    }

    private void incrementSuccessfulRestoreCount() {
        successfulRestoreCount.incrementAndGet();
    }

    private void incrementErrorRestoreCount() {
        errorRestoreCount.incrementAndGet();
    }

    @Override
    public TaskType type() {
        return TYPE;
    }

    @Override
    public Optional<TaskExecutionDetails.AdditionalInformation> details() {
        return Optional.of(new AdditionalInformation(userToRestore, successfulRestoreCount.get(), errorRestoreCount.get()));
    }

    User getUserToRestore() {
        return userToRestore;
    }
}
