package org.apache.james.jmap.model;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.NotImplementedException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

@JsonDeserialize(builder = UpdateMessagePatch.Builder.class)
public class UpdateMessagePatch {

    private final List<String> mailboxIds;
    private final Optional<Boolean> isUnread;
    private final Optional<Boolean> isFlagged;
    private final Optional<Boolean> isAnswered;

    public static Builder builder() {
        return new Builder();
    }

    @VisibleForTesting
    UpdateMessagePatch(List<String> mailboxIds,
                       Optional<Boolean> isUnread,
                       Optional<Boolean> isFlagged,
                       Optional<Boolean> isAnswered) {

        this.mailboxIds = mailboxIds;
        this.isUnread = isUnread;
        this.isFlagged = isFlagged;
        this.isAnswered = isAnswered;
    }

    public List<String> getMailboxIds() {
        return mailboxIds;
    }

    public Optional<Boolean> isUnread() {
        return isUnread;
    }

    public Optional<Boolean> isFlagged() {
        return isFlagged;
    }

    public Optional<Boolean> isAnswered() {
        return isAnswered;
    }


    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private ImmutableList.Builder<String> mailboxIds = ImmutableList.builder();
        private Optional<Boolean> isFlagged = Optional.empty();
        private Optional<Boolean> isUnread = Optional.empty();
        private Optional<Boolean> isAnswered = Optional.empty();

        public Builder mailboxIds(Optional<List<String>> mailboxIds) {
            if (mailboxIds.isPresent()) {
                throw new NotImplementedException();
            }
            return this;
        }

        public Builder isFlagged(Optional<Boolean> isFlagged) {
            this.isFlagged = isFlagged;
            return this;
        }

        public Builder isUnread(Optional<Boolean> isUnread) {
            this.isUnread = isUnread;
            return this;
        }

        public Builder isAnswered(Optional<Boolean> isAnswered) {
            this.isAnswered = isAnswered;
            return this;
        }

        public UpdateMessagePatch build() {

            return new UpdateMessagePatch(mailboxIds.build(), isUnread, isFlagged, isAnswered);
        }
    }

    public boolean isValid() {
        return true; // to be implemented when UpdateMessagePatch would allow any message property to be set
    }
}
