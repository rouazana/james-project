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
package org.apache.james.queue.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.http.entity.SerializableEntity;
import org.junit.jupiter.api.Test;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectSerializer;
import org.nustaq.serialization.serializers.FSTMapSerializer;
import org.nustaq.serialization.util.FSTUtil;

import com.esotericsoftware.jsonbeans.Json;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.WritableTypeId.Inclusion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.mikerusoft.jsonable.parser.JsonReader;
import com.mikerusoft.jsonable.parser.JsonWriter;

public class JsonableTest {

    static final String JSON_TYPE = "@type";
//
//    @Test
//    public void serializeShouldWork() throws Exception {
//        MyClass myClass = new MyClass("bar");
//        
//        ObjectMapper objectMapper = new ObjectMapper();
//        String json = objectMapper.writeValueAsString(myClass);
//        
//        String expectedJson = "{\"@type\":\"org.apache.james.queue.rabbitmq.JsonableTest$MyClass\",\"foo\":\"bar\"}";
//        
//        assertThat(json).isEqualTo(expectedJson);
//    }
//
//    @Test
//    public void deserializeShouldWork() throws Exception {
//        String json = "{\"@type\":\"org.apache.james.queue.rabbitmq.JsonableTest$MyClass\",\"foo\":\"bar\"}";
//        ObjectMapper objectMapper = new ObjectMapper();
//        JsonNode jsonNode = objectMapper.readTree(json);
//
//        String type = jsonNode.get("@type").asText();
//
//        Class<?> clazz = Class.forName(type);
//
//        MyClass myClass = (MyClass) objectMapper.convertValue(jsonNode, clazz);
//        assertThat(myClass.foo).isEqualTo("bar");
//    }
//
//    @Test
//    public void chainingShouldWork() throws Exception {
//        MyClass expected = new MyClass("bar");
//        
//        ObjectMapper objectMapper = new ObjectMapper();
//        String json = objectMapper.writeValueAsString(expected);
//        
//        JsonNode jsonNode = objectMapper.readTree(json);
//
//        String type = jsonNode.get("@type").asText();
//
//        Class<?> clazz = Class.forName(type);
//
//        MyClass myClass = (MyClass) objectMapper.convertValue(jsonNode, clazz);
//        assertThat(myClass.foo).isEqualTo("bar");
//    }
//
//    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
//            include = JsonTypeInfo.As.PROPERTY,
//            property = JSON_TYPE)
//    private static class MyClass {
//
//        @JsonProperty
//        private final String foo;
//
//        @JsonCreator
//        public MyClass(@JsonProperty("foo") String foo) {
//            this.foo = foo;
//        }
//
//        @Override
//        public int hashCode() {
//            return Objects.hashCode(foo);
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            if (obj instanceof MyClass) {
//                MyClass other = (MyClass) obj;
//                return Objects.equals(foo, other.foo);
//            }
//            return false;
//        }
//
//        @Override
//        public String toString() {
//            return MoreObjects.toStringHelper(MyClass.class)
//                    .add("foo", foo)
//                    .toString();
//        }
//    }
//
//    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
//            include = JsonTypeInfo.As.PROPERTY,
//            property = JSON_TYPE)
//    private static class MyClassWithMap {
//
//        @JsonProperty
//        private final Map<Integer, String> foo;
//
//        @JsonCreator
//        public MyClassWithMap(@JsonProperty("foo") Map<Integer, String> foo) {
//            this.foo = foo;
//        }
//
//        @Override
//        public int hashCode() {
//            return Objects.hashCode(foo);
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            if (obj instanceof MyClass) {
//                MyClass other = (MyClass) obj;
//                return Objects.equals(foo, other.foo);
//            }
//            return false;
//        }
//
//        @Override
//        public String toString() {
//            return MoreObjects.toStringHelper(MyClass.class)
//                    .add("foo", foo)
//                    .toString();
//        }
//    }
//
//    @Test
//    public void serializeMapAsObjectShouldWork() throws Exception {
//        MyClassWithMap myClass = new MyClassWithMap(ImmutableMap.of(1, "bar", 2, "bar2"));
//        
//        ObjectMapper objectMapper = new ObjectMapper();
//        String json = objectMapper.writeValueAsString(myClass);
//        
//        String expectedJson = "{\"@type\":\"org.apache.james.queue.rabbitmq.JsonableTest$MyClassWithMap\",\"foo\":{\"1\":\"bar\",\"2\":\"bar2\"}}";
//        
//        assertThat(json).isEqualTo(expectedJson);
//    }
//
//    @Test
//    public void deserializeMapAsObjectShouldWork() throws Exception {
//        String json = "{\"@type\":\"org.apache.james.queue.rabbitmq.JsonableTest$MyClassWithMap\",\"foo\":{\"1\":\"bar\",\"2\":\"bar2\"}}";
//        ObjectMapper objectMapper = new ObjectMapper();
//        JsonNode jsonNode = objectMapper.readTree(json);
//
//        String type = jsonNode.get("@type").asText();
//
//        Class<?> clazz = Class.forName(type);
//
//        MyClassWithMap myClass = (MyClassWithMap) objectMapper.convertValue(jsonNode, clazz);
//        assertThat(myClass.foo).containsAllEntriesOf(ImmutableMap.of(1, "bar", 2, "bar2"));
//    }

//    @Test
//    public void serializeMapShouldWork() throws Exception {
//        HashMap<Integer,String> myClass = new HashMap<>();
//        myClass.put(1, "bar");
//        myClass.put(2, "bar2");
//        
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
//        String json = objectMapper.writeValueAsString(myClass);
//        
//        String expectedJson = "{\"@type\":\"java.util.Hashmap\",{\"1\":\"bar\",\"2\":\"bar2\"}}";
//        
//        assertThat(json).isEqualTo(expectedJson);
//    }
    
    @Test
    void serializeMapShouldWork() throws Exception {
        HashMap<Integer, String> map = new HashMap<>();
        map.put(1, "foo");
        map.put(2, "bar");
        
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enableDefaultTypingAsProperty(ObjectMapper.DefaultTyping.NON_FINAL, JSON_TYPE);
        String json = objectMapper.writeValueAsString(map);
        System.out.println(json);
        
        JsonNode jsonNode = objectMapper.readTree(json);
        String type = jsonNode.get("@type").asText();
        
        Class<?> clazz = Class.forName(type);

        Object actual = objectMapper.convertValue(jsonNode, clazz);

        assertThat(actual).isEqualTo(map);
      }

    
    @Test
    void serializeImmutableMapShouldWork() throws Exception {
        ImmutableMap<Integer, String> map = ImmutableMap.of(1, "foo", 2, "bar");
        
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new GuavaModule());
//        objectMapper.enableDefaultTypingAsProperty(ObjectMapper.DefaultTyping.NON_FINAL, JSON_TYPE);
        StdTypeResolverBuilder resolver = new StdTypeResolverBuilder()
            .init(JsonTypeInfo.Id.CLASS, null)
            .inclusion(JsonTypeInfo.As.PROPERTY);
        objectMapper.setDefaultTyping(resolver);
        String json = objectMapper.writeValueAsString(map);
        System.out.println(json);
        
        JsonNode jsonNode = objectMapper.readTree(json);
        String type = jsonNode.get("@type").asText();
        
        Class<?> clazz = Class.forName(type);

        Object actual = objectMapper.convertValue(jsonNode, clazz);

        assertThat(actual).isEqualTo(map);
      }

    @Test
    void serializeMapShouldWorkWithJsonBeans() throws Exception {
        HashMap<Integer, String> map = new HashMap<>();
        map.put(1, "foo");
        map.put(2, "bar");
        
        Json json = new Json();
        String serialized = json.toJson(map);
        System.out.println(serialized);
        
        Object actual = json.fromJson(Object.class, serialized);
        
        assertThat(actual).isEqualTo(map);
      }

    @Test
    void serializeMapShouldWorkWithJsonable() throws Exception {
        HashMap<Integer, String> map = new HashMap<>();
        map.put(1, "foo");
        map.put(2, "bar");
        
        StringBuilder stringBuilder = new StringBuilder();
        JsonWriter.write(map, stringBuilder);
        String serialized = stringBuilder.toString();
        System.out.println(serialized);
        
        Object actual = JsonReader.read(serialized);
        
        assertThat(actual).isEqualTo(map);
      }

    @Test
    void serializeImmutableMapShouldWorkWithJsonable() throws Exception {
        ImmutableMap<Integer, String> map = ImmutableMap.of(1, "foo", 2, "bar");
        
        StringBuilder stringBuilder = new StringBuilder();
        JsonWriter.write(map, stringBuilder);
        String serialized = stringBuilder.toString();
        System.out.println(serialized);
        
        Object actual = JsonReader.read(serialized);
        
        assertThat(actual).isEqualTo(map);
      }

    @Test
    void serializeMapShouldWorkWithKryo() throws Exception {
        HashMap<Integer, String> map = new HashMap<>();
        map.put(1, "foo");
        map.put(2, "bar");
        
        Kryo kryo = new Kryo();
        kryo.register(HashMap.class);
        kryo.register(String.class);
        kryo.register(Integer.class);
        Output output = new Output(new byte[] {}, -1);
        kryo.writeObject(output, map);
        System.out.println(new String(output.toBytes()));
        
        Input input = new Input(output.toBytes());
        
        Object actual = kryo.readClassAndObject(input);
        
        assertThat(actual).isEqualTo(map);
      }


    @Test
    void serializeMapShouldWorkWithGSon() throws Exception {
        HashMap<Integer, String> map = new HashMap<>();
        map.put(1, "foo");
        map.put(2, "bar");
        
        Gson gson = new Gson();
        String serialized = gson.toJson(map);
        System.out.println(serialized);
        
        Object actual = gson.fromJson(serialized, Object.class);
        
        assertThat(actual).isEqualTo(map);
      }

    @Test
    void serializeImmutableMapShouldWorkWithGSonHackyVersion() throws Exception {
        ImmutableMap<Integer, String> map = ImmutableMap.of(1, "foo", 2, "bar");
        
        Gson gson = new Gson();
        String serialized = gson.toJson(map);
        System.out.println(serialized);
        
        Object actual = gson.fromJson(serialized, new TypeToken<Map<Integer, String>>() {}.getType());
        
        assertThat(actual).isEqualTo(map);
      }

    @Test
    void serializeMapShouldWorkWithFst() throws Exception {
        HashMap<Integer, String> map = new HashMap<>();
        map.put(1, "foo");
        map.put(2, "bar");
        
        FSTConfiguration conf = FSTConfiguration.createJsonConfiguration();
        byte[] asByteArray = conf.asByteArray(map);
        String serialized = new String(asByteArray, Charsets.UTF_8);
        System.out.println(serialized);
        
        Object actual = conf.asObject(asByteArray);
        
        assertThat(actual).isEqualTo(map);
      }

    @Test
    void serializeImmutableMapShouldWorkWithFst() throws Exception {
        ImmutableMap<Integer, String> map = ImmutableMap.of(1, "foo", 2, "bar");
        
        FSTConfiguration conf = FSTConfiguration.createJsonConfiguration();
        FSTObjectSerializer ser = new FSTJSonImmutableMapSerializer();
        conf.registerSerializer(ImmutableMap.class, ser, true);
        JsonFactory jacksonImplementation = conf.getCoderSpecific();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new GuavaModule());
        jacksonImplementation.setCodec(objectMapper);
        conf.setCoderSpecific(jacksonImplementation);
        byte[] asByteArray = conf.asByteArray(map);
        String serialized = new String(asByteArray, Charsets.UTF_8);
        System.out.println(serialized);
        
        Object actual = conf.asObject(asByteArray);
        
        assertThat(actual).isEqualTo(map);
      }


    @Test
    void serializeImmutableMapShouldWorkWithFstWithoutJson() throws Exception {
        ImmutableMap<Integer, String> map = ImmutableMap.of(1, "foo", 2, "bar");
        
        FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
        byte[] asByteArray = conf.asByteArray(map);
        String serialized = new String(asByteArray, Charsets.UTF_8);
        System.out.println(serialized);
        
        Object actual = conf.asObject(asByteArray);
        
        assertThat(actual).isEqualTo(map);
      }


    @Test
    void serializeMyClassShouldWorkWithFst() throws Exception {
        MyClass myClass = new MyClass("youpi");
        
        FSTConfiguration conf = FSTConfiguration.createJsonConfiguration();
        conf.registerCrossPlatformClassMappingUseSimpleName(ImmutableMap.class);
        byte[] asByteArray = conf.asByteArray(myClass);
        String serialized = new String(asByteArray, Charsets.UTF_8);
        System.out.println(serialized);
        
        Object actual = conf.asObject(asByteArray);
        
        assertThat(actual).isEqualTo(myClass);
      }

    
    //    @Test
//    public void deserializeMapShouldWork() throws Exception {
//        String json = "{\"@type\":\"org.apache.james.queue.rabbitmq.JsonableTest$MyClassWithMap\",\"foo\":{\"1\":\"bar\",\"2\":\"bar2\"}}";
//        ObjectMapper objectMapper = new ObjectMapper();
//        JsonNode jsonNode = objectMapper.readTree(json);
//
//        String type = jsonNode.get("@type").asText();
//
//        Class<?> clazz = Class.forName(type);
//
//        MyClassWithMap myClass = (MyClassWithMap) objectMapper.convertValue(jsonNode, clazz);
//        assertThat(myClass.foo).containsAllEntriesOf(ImmutableMap.of(1, "bar", 2, "bar2"));
//    }
    private static class MyClass implements Serializable {
        private final String yop;
        
        public MyClass(String yop) {
            this.yop = yop;
        }

        public String getYop() {
            return yop;
        }
        
        @Override
        public boolean equals(Object other) {
            return (other instanceof MyClass) && (((MyClass)other).yop.equals(yop)); 
        }
    }
    public static class FSTJSonImmutableMapSerializer extends FSTMapSerializer {

        public static final Class<?> UNMODIFIABLE_MAP_CLASS;

        static {
            UNMODIFIABLE_MAP_CLASS = ImmutableMap.of().getClass();
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public Object instantiate(Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws Exception {
            try {
                // note: unlike with list's JDK uses a single wrapper for unmodifiable maps, so information regarding ordering gets lost.
                // as the enclosed map is private, there is also no possibility to detect that case
                // we could always create a linkedhashmap here, but this would have major performance drawbacks.

                // this only hits JSON codec as JSON codec does not implement a full JDK-serialization fallback (like the binary codecs)
                int len = in.readInt();
                if (UNMODIFIABLE_MAP_CLASS.isAssignableFrom(objectClass)) {
                    ImmutableMap.Builder res = ImmutableMap.builderWithExpectedSize(len);
                    in.registerObject(res, streamPosition, serializationInfo, referencee);
                    for (int i = 0; i < len; i++) {
                        Object key = in.readObjectInternal((Class)null);
                        Object val = in.readObjectInternal((Class)null);
                        res.put(key, val);
                    }
                    return res.build();
                }
            } catch (Throwable th) {
                FSTUtil.<RuntimeException>rethrow(th);
            }
            return null;
        }

    }

}
