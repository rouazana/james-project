package org.apache.james.webadmin.dto;

import java.io.IOException;

import org.apache.james.mailbox.quota.QuotaValue;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class QuotaValueSerializer<T extends QuotaValue<T>> extends JsonSerializer<T> {

    @Override
    public void serialize(T value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
        generator.writeNumber(serialize(value));
    }

    private long serialize(T value) {
        if (value.isUnlimited()) {
            return -1;
        }
        return value.asLong();
    }
}
