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

package org.apache.james.jmap.methods;

import org.apache.james.jmap.MethodsModule;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class TestMethodsModule extends AbstractModule {

    @Override
    @SuppressWarnings("rawtypes")
    protected void configure() {
        install(new MethodsModule());

        Multibinder<Method> methods = Multibinder.newSetBinder(binder(), Method.class);
        methods.addBinding().to(MyMethod.class);
    }

    public static class MyJmapRequest implements JmapRequest {

        public String id;
        public String name;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public static class MyJmapResponse implements JmapResponse {

        private final String id;
        private final String name;
        private final String message;

        public MyJmapResponse(String id, String name, String message) {
            this.id = id;
            this.name = name;
            this.message = message;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class MyMethod implements Method<MyJmapRequest, MyJmapResponse> {

        @Override
        public String methodName() {
            return "myMethod";
        }

        @Override
        public MyJmapResponse process(MyJmapRequest request) {
            return new MyJmapResponse(request.getId(), request.getName(), "works");
        }

        @Override
        public Class<MyJmapRequest> requestClass() {
            return MyJmapRequest.class;
        }
    }
}