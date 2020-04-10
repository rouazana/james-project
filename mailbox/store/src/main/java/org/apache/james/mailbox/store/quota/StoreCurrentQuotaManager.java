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

import org.apache.james.mailbox.model.CurrentQuotas;
import org.apache.james.mailbox.model.QuotaOperation;
import org.apache.james.mailbox.quota.CurrentQuotaManager;

import reactor.core.publisher.Mono;

public interface StoreCurrentQuotaManager extends CurrentQuotaManager {

    Mono<Void> increase(QuotaOperation quotaOperation);

    Mono<Void> decrease(QuotaOperation quotaOperation);

    default Mono<Void> resetCurrentQuotas(QuotaOperation quotaOperation) {
        return Mono.from(getCurrentQuotas(quotaOperation.quotaRoot()))
            .flatMap(storedQuotas -> {
                if (!storedQuotas.equals(CurrentQuotas.from(quotaOperation))) {
                    return decrease(new QuotaOperation(quotaOperation.quotaRoot(), storedQuotas.count(), storedQuotas.size()))
                        .then(increase(quotaOperation));
                }
                return Mono.empty();
            });
    }

}
