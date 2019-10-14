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
package org.apache.james.task;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import com.github.fge.lambdas.functions.ThrowingFunction;

public class MemoryReferenceWithCounterTask implements Task {
    public static final TaskType TYPE = TaskType.of("memory-reference-task-with-counter");

    public static class AdditionalInformation implements TaskExecutionDetails.AdditionalInformation {
        private final long count;

        public AdditionalInformation(long count) {
            this.count = count;
        }

        public long getCount() {
            return count;
        }

        @Override
        public boolean equals(Object that) {
            if(that instanceof MemoryReferenceWithCounterTask.AdditionalInformation) {
                return Objects.equals(this.count, ((AdditionalInformation) that).getCount());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.count);
        }
    }

    private final ThrowingFunction<AtomicLong, Result> task;
    private final AtomicLong counter = new AtomicLong(0);

    public MemoryReferenceWithCounterTask(ThrowingFunction<AtomicLong, Result> task) {
        this.task = task;
    }

    @Override
    public Result run() {
        try {
            return task.apply(counter);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    @Override
    public TaskType type() {
        return TYPE;
    }

    @Override
    public Optional<TaskExecutionDetails.AdditionalInformation> details() {
        return Optional.of(new MemoryReferenceWithCounterTask.AdditionalInformation(counter.get()));
    }
}
