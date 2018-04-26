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

package org.apache.james.mailbox.inmemory.quota;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import org.apache.james.core.User;
import org.apache.james.mailbox.quota.QuotaThresholdChangesStore;
import org.apache.james.mailbox.quota.model.QuotaThresholdChange;
import org.apache.james.mailbox.quota.model.QuotaThresholdChanges;

public class InMemoryQuotaThresholdChangesStore implements QuotaThresholdChangesStore {
    private static final int MAX_RETRY = 50;

    private final ConcurrentHashMap<User, QuotaThresholdChanges> countChanges;
    private final ConcurrentHashMap<User, QuotaThresholdChanges> sizeChanges;

    public InMemoryQuotaThresholdChangesStore() {
        countChanges = new ConcurrentHashMap<>();
        sizeChanges = new ConcurrentHashMap<>();
    }

    @Override
    public QuotaThresholdChanges retrieveQuotaSizeThresholdChanges(User user) {
        return Optional.ofNullable(sizeChanges.get(user))
            .orElse(new QuotaThresholdChanges());
    }

    @Override
    public void persistQuotaSizeThresholdChange(User user, QuotaThresholdChange change) {
        doAddChange(sizeChanges, user, change);
    }

    @Override
    public QuotaThresholdChanges retrieveQuotaCountThresholdChanges(User user) {
        return Optional.ofNullable(countChanges.get(user))
            .orElse(new QuotaThresholdChanges());
    }

    @Override
    public void persistQuotaCountThresholdChange(User user, QuotaThresholdChange change) {
        doAddChange(countChanges, user, change);
    }

    private void doAddChange(ConcurrentHashMap<User, QuotaThresholdChanges> map, User user, QuotaThresholdChange change) {
        IntStream.range(0, MAX_RETRY)
            .filter(i -> tryAddChange(map, user, change))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Failure to persist change after several trial"));
    }

    private boolean tryAddChange(ConcurrentHashMap<User, QuotaThresholdChanges> map, User user, QuotaThresholdChange change) {
        Optional<QuotaThresholdChanges> currentState = Optional.ofNullable(map.get(user));

        QuotaThresholdChanges newState = currentState
            .map(state -> state.combineWith(change))
            .orElse(new QuotaThresholdChanges(change));

        if (currentState.isPresent()) {
            return map.replace(user, currentState.get(), newState);
        } else {
            return map.putIfAbsent(user, newState) == null;
        }
    }
}
