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

package org.apache.james.modules.objectstorage.guice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.configuration.MapConfiguration;
import org.apache.james.GuiceModuleTestRule;
import org.apache.james.blob.api.BlobStore;
import org.apache.james.blob.api.HashBlobId;
import org.apache.james.blob.objectstorage.ContainerName;
import org.apache.james.blob.objectstorage.ObjectStorageBlobsDAO;
import org.apache.james.blob.objectstorage.PayloadCodec;
import org.apache.james.blob.objectstorage.swift.Credentials;
import org.apache.james.blob.objectstorage.swift.SwiftKeystone2ObjectStorage;
import org.apache.james.blob.objectstorage.swift.TenantName;
import org.apache.james.blob.objectstorage.swift.UserName;
import org.apache.james.modules.objectstorage.PayloadCodecs;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.GenericContainer;

import com.github.fge.lambdas.Throwing;
import com.google.inject.Module;
import com.google.inject.util.Modules;

public class DockerSwiftTestRule implements GuiceModuleTestRule {

    private org.apache.james.blob.objectstorage.DockerSwiftRule swiftContainer =
        new org.apache.james.blob.objectstorage.DockerSwiftRule();
    private final PayloadCodec payloadCodec;

    public DockerSwiftTestRule() {
        this(PayloadCodecs.DEFAULT);
    }

    public DockerSwiftTestRule(PayloadCodecs payloadCodec) {
        Map<String, Object> payloadCodecConfig = new HashMap<>();
        payloadCodecConfig.put(PayloadCodecs.OBJECTSTORAGE_AES256_HEXSALT, "c603a7327ee3dcbc031d8d34b1096c605feca5e1");
        payloadCodecConfig.put(PayloadCodecs.OBJECTSTORAGE_AES256_PASSWORD, "dockerSwiftEncryption");
        this.payloadCodec = payloadCodec.codec(new MapConfiguration(payloadCodecConfig));
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return swiftContainer.apply(base, description);
    }

    @Override
    public void await() {
    }

    @Override
    public Module getModule() {
        SwiftKeystone2ObjectStorage.Configuration configuration = SwiftKeystone2ObjectStorage.configBuilder()
            .credentials(Credentials.of("demo"))
            .tenantName(TenantName.of("test"))
            .userName(UserName.of("demo"))
            .endpoint(swiftContainer.dockerSwift().keystoneV2Endpoint())
            .build();

        ContainerName containerName = ContainerName.of(UUID.randomUUID().toString());
        ObjectStorageBlobsDAO dao = SwiftKeystone2ObjectStorage.daoBuilder(configuration)
            .blobIdFactory(new HashBlobId.Factory())
            .container(containerName)
            .payloadCodec(payloadCodec)
            .build();

        Throwing.supplier(() -> dao.createContainer(containerName).get()).sneakyThrow().get();
        return Modules.combine((binder) -> binder.bind(BlobStore.class).toInstance(dao));
    }


    public void start() {
        swiftContainer.start();
    }

    public void stop() {
        swiftContainer.stop();
    }

    public GenericContainer<?> getRawContainer() {
        return swiftContainer.getRawContainer();
    }

}