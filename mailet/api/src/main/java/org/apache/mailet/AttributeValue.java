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

package org.apache.mailet;

import java.util.Collection;
import java.util.Objects;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.james.util.streams.Iterators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableList;

/** 
 * Strong typing for attribute value, which represent the value of an attribute stored in a mail.
 * 
 * @since Mailet API v3.2
 */
public class AttributeValue<T> {
    private final T value;
    private final Serializer<T> serializer;

    public static AttributeValue<String> of(String value) {
        return new AttributeValue<>(value, Serializer.STRING_SERIALIZER);
    }

    public static AttributeValue<Integer> of(Integer value) {
        return new AttributeValue<>(value, Serializer.INT_SERIALIZER);
    }

    public static <U> AttributeValue<Collection<U>> of(Collection<U> value) {
        return new AttributeValue<>(value, new Serializer.CollectionSerializer<U>());
    }

    public static <V> AttributeValue<V> of(V otherValue) {
        throw new NotImplementedException("comming soon?");
    }

    private AttributeValue(T value, Serializer<T> serializer) {
        this.value = value;
        this.serializer = serializer;
    }

    public JsonNode toJson() {
        return serializer.serialize(value);
    }

    public static AttributeValue<String> fromJson(TextNode stringAsJson) {
        return AttributeValue.of(stringAsJson.asText());
    }

    public static AttributeValue<Integer> fromJson(IntNode intAsJson) {
        return AttributeValue.of(intAsJson.asInt());
    }

    public static AttributeValue<Collection<Object>> fromJson(ArrayNode arrayAsJson) {
        return AttributeValue.of(
            Iterators.toStream(arrayAsJson.elements())
                .map(AttributeValue::fromJson)
                .collect(ImmutableList.toImmutableList()));
    }

    public static <X> AttributeValue<X> fromJson(JsonNode otherJson) {
        throw new NotImplementedException("comming soon?");
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof AttributeValue) {
            AttributeValue<?> that = (AttributeValue<?>) o;

            return Objects.equals(this.value, that.value);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(value);
    }
}
