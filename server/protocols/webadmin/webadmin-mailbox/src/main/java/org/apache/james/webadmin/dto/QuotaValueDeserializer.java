package org.apache.james.webadmin.dto;

import java.io.IOException;
import java.util.function.Function;

import org.apache.james.mailbox.quota.QuotaValue;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class QuotaValueDeserializer<T extends QuotaValue<T>> extends JsonDeserializer<T> {

    private final T unlimited;
    private final Function<Long, T> quotaFactory;

    public QuotaValueDeserializer(T unlimited, Function<Long, T> quotaFactory) {
        this.unlimited = unlimited;
        this.quotaFactory = quotaFactory;
    }

    @Override
    public T deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        return deserialize(parser.getValueAsLong());
    }

    private T deserialize(Long value) {
        if (value == -1) {
            return unlimited;
        }
        return quotaFactory.apply(value);
    }

}
