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

package org.apache.mailbox.tools.indexer;

import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;

import org.apache.james.json.DTOModule;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.server.task.json.dto.TaskDTO;
import org.apache.james.server.task.json.dto.TaskDTOModule;
import org.apache.james.task.Task;
import org.apache.james.task.TaskExecutionDetails;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SingleMailboxReindexingTask implements Task {

    public static class SingleMailboxReindexingTaskDTO implements TaskDTO {

        public static SingleMailboxReindexingTask.SingleMailboxReindexingTaskDTO of(SingleMailboxReindexingTask task, String typeName) {
            return new SingleMailboxReindexingTask.SingleMailboxReindexingTaskDTO(typeName, task.mailboxId.serialize());
        }

        private final String type;
        private final String mailboxId;

        public SingleMailboxReindexingTaskDTO(@JsonProperty("type") String type, @JsonProperty("mailboxId") String mailboxId) {
            this.type = type;
            this.mailboxId = mailboxId;
        }

        @Override
        public String getType() {
            return type;
        }

        public String getMailboxId() {
            return mailboxId;
        }

    }

    public static final String MAILBOX_RE_INDEXING = "mailboxReIndexing";

    public static final Function<Factory, TaskDTOModule<SingleMailboxReindexingTask, SingleMailboxReindexingTaskDTO>> MODULE = (factory) ->
        DTOModule
            .forDomainObject(SingleMailboxReindexingTask.class)
            .convertToDTO(SingleMailboxReindexingTaskDTO.class)
            .toDomainObjectConverter(factory::create)
            .toDTOConverter(SingleMailboxReindexingTaskDTO::of)
            .typeName(MAILBOX_RE_INDEXING)
            .withFactory(TaskDTOModule::new);

    public static class AdditionalInformation extends ReprocessingContextInformation {
        private final MailboxId mailboxId;

        AdditionalInformation(MailboxId mailboxId, ReprocessingContext reprocessingContext) {
            super(reprocessingContext);
            this.mailboxId = mailboxId;
        }

        public String getMailboxId() {
            return mailboxId.serialize();
        }
    }

    public static class Factory {

        private final ReIndexerPerformer reIndexerPerformer;
        private final MailboxId.Factory mailboxIdFactory;

        @Inject
        public Factory(ReIndexerPerformer reIndexerPerformer, MailboxId.Factory mailboxIdFactory) {
            this.reIndexerPerformer = reIndexerPerformer;
            this.mailboxIdFactory = mailboxIdFactory;
        }

        public SingleMailboxReindexingTask create(SingleMailboxReindexingTaskDTO dto) {
            MailboxId mailboxId = mailboxIdFactory.fromString(dto.getMailboxId());
            return new SingleMailboxReindexingTask(reIndexerPerformer, mailboxId);
        }
    }

    private final ReIndexerPerformer reIndexerPerformer;
    private final MailboxId mailboxId;
    private final AdditionalInformation additionalInformation;
    private final ReprocessingContext reprocessingContext;

    @Inject
    public SingleMailboxReindexingTask(ReIndexerPerformer reIndexerPerformer, MailboxId mailboxId) {
        this.reIndexerPerformer = reIndexerPerformer;
        this.mailboxId = mailboxId;
        this.reprocessingContext = new ReprocessingContext();
        this.additionalInformation = new AdditionalInformation(mailboxId, reprocessingContext);
    }

    @Override
    public Result run() {
        try {
            return reIndexerPerformer.reIndex(mailboxId, reprocessingContext);
        } catch (Exception e) {
            return Result.PARTIAL;
        }
    }

    @Override
    public String type() {
        return MAILBOX_RE_INDEXING;
    }

    @Override
    public Optional<TaskExecutionDetails.AdditionalInformation> details() {
        return Optional.of(additionalInformation);
    }

}
