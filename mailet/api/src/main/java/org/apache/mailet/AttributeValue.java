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

import java.util.Objects;

/** 
 * Strong typing for attribute value, which represent the value of an attribute stored in a mail.
 * 
 * @since Mailet API v3.2
 */
public class AttributeValue<T> {
    private final T value;
    private final SerDes<T> serDes;

    public static AttributeValue<String> of(String value) {
        return new AttributeValue<>(value, SerDes.STRING_SER_DES);
    }

    public static AttributeValue<Integer> of(Integer value) {
        return new AttributeValue<>(value, SerDes.INT_SER_DES);
    }

    private AttributeValue(T value, SerDes<T> serDes) {
        this.value = value;
        this.serDes = serDes;
    }

    public String toJson() {
        return serDes.serialize(value);
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
