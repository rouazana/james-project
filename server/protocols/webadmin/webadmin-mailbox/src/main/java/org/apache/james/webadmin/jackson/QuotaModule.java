package org.apache.james.webadmin.jackson;

import org.apache.james.mailbox.quota.QuotaCount;
import org.apache.james.mailbox.quota.QuotaSize;
import org.apache.james.webadmin.dto.QuotaValueDeserializer;
import org.apache.james.webadmin.dto.QuotaValueSerializer;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class QuotaModule extends SimpleModule {

    public QuotaModule() {
        addSerializer(QuotaSize.class, new QuotaValueSerializer<>());
        addSerializer(QuotaCount.class, new QuotaValueSerializer<>());
        addDeserializer(QuotaSize.class, new QuotaValueDeserializer<>(QuotaSize.unlimited(), QuotaSize::size));
        addDeserializer(QuotaCount.class, new QuotaValueDeserializer<>(QuotaCount.unlimited(), QuotaCount::count));
    }

}
