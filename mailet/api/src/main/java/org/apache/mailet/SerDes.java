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

/** 
 * Serializer and Deserializer
 * 
 * @since Mailet API v3.2
 */
public interface SerDes<T> {
    String serialize(T object);

    T deserialize(String json);

    class StringSerDes implements SerDes<String> {

        @Override
        public String serialize(String object) {
            return object;
        }

        @Override
        public String deserialize(String json) {
            return json;
        }
    }

    SerDes<String> STRING_SER_DES = new StringSerDes();

    class IntSerDes implements SerDes<Integer> {

        @Override
        public String serialize(Integer object) {
            return String.valueOf(object);
        }

        @Override
        public Integer deserialize(String json) {
            return Integer.valueOf(json);
        }
    }

    SerDes<Integer> INT_SER_DES = new IntSerDes();
}
