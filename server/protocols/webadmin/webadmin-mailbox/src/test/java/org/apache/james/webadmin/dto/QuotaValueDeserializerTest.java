package org.apache.james.webadmin.dto;

import org.apache.james.mailbox.quota.QuotaCount;
import org.apache.james.mailbox.quota.QuotaSize;
import org.apache.james.webadmin.utils.JsonExtractException;
import org.apache.james.webadmin.utils.JsonExtractor;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class QuotaValueDeserializerTest {

    @Test
    public void objectDeserializeShouldContainGivenValues() throws JsonExtractException {
        String payload = "{\"count\":52,\"size\":42}";
        QuotaDTO actual = new JsonExtractor<>(QuotaDTO.class,
            new SimpleModule()
                .addDeserializer(QuotaCount.class, new QuotaValueDeserializer<>(QuotaCount.unlimited(), QuotaCount::count))
                .addDeserializer(QuotaSize.class, new QuotaValueDeserializer<>(QuotaSize.unlimited(), QuotaSize::size))
        ).parse(payload);
        Assertions.assertThat(actual)
            .isEqualTo(
                QuotaDTO
                    .builder()
                    .count(java.util.Optional.of(QuotaCount.count(52)))
                    .size(java.util.Optional.of(QuotaSize.size(42)))
                    .build());
    }

}