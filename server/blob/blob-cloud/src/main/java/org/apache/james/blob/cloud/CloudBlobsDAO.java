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

package org.apache.james.blob.cloud;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.IOUtils;
import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.api.HashBlobId;
import org.apache.james.blob.api.ObjectStore;
import org.apache.james.blob.api.ObjectStoreException;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.options.CopyOptions;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.swift.v1.blobstore.RegionScopedBlobStoreContext;

import com.github.fge.lambdas.Throwing;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.inject.Inject;
import com.google.inject.Module;

class CloudBlobsDAO implements ObjectStore {
    private static final InputStream EMPTY_STREAM = new ByteArrayInputStream(new byte[0]);
    private static final Iterable<Module> JCLOUDS_MODULES = ImmutableSet.of(new SLF4JLoggingModule());

    private final BlobId.Factory blobIdFactory;
    private final BlobStore blobStore;
    private final ContainerName containerName;

    @Inject
    public CloudBlobsDAO(ContainerName containerName, HashBlobId.Factory blobIdFactory,
                         CloudsBlobsConfiguration cloudsBlobsConfiguration) {
        this.blobIdFactory = blobIdFactory;
        this.containerName = containerName;

        RegionScopedBlobStoreContext blobStoreContext = ContextBuilder.newBuilder("openstack-swift")
            .endpoint(cloudsBlobsConfiguration.getEndpoint().toString())
            .credentials(
                cloudsBlobsConfiguration.getIdentity().getValue(),
                cloudsBlobsConfiguration.getCredentials().getValue())
            .overrides(cloudsBlobsConfiguration.getOverrides())
            .modules(JCLOUDS_MODULES)
            .buildView(RegionScopedBlobStoreContext.class);

        blobStore = cloudsBlobsConfiguration
            .getRegion()
            .map(region -> blobStoreContext.getBlobStore(region.getValue()))
            .orElse(blobStoreContext.getBlobStore());
    }

    @Override
    public CompletableFuture<BlobId> save(byte[] data) {
        return save(new ByteArrayInputStream(data));
    }

    @Override
    public CompletableFuture<BlobId> save(InputStream data) {
        Preconditions.checkNotNull(data);
        HashingInputStream hashingInputStream = new HashingInputStream(Hashing.sha256(), data);
        String tmpId = UUID.randomUUID().toString();
        String containerName = this.containerName.getValue();

        Blob blob = blobStore.blobBuilder(tmpId).payload(hashingInputStream).build();
        blobStore.putBlob(containerName, blob);
        BlobId id = blobIdFactory.from(hashingInputStream.hash().toString());
        blobStore.copyBlob(containerName, tmpId, containerName, id.asString(), CopyOptions.NONE);
        blobStore.removeBlob(containerName, tmpId);

        return CompletableFuture.completedFuture(id);
    }

    @Override
    public CompletableFuture<byte[]> readBytes(BlobId blobId) {
        return CompletableFuture
            .supplyAsync(Throwing.supplier(() -> IOUtils.toByteArray(read(blobId))).sneakyThrow());
    }

    @Override
    public InputStream read(BlobId blobId) {
        Blob blob = blobStore.getBlob(containerName.getValue(), blobId.asString());

        InputStream is = EMPTY_STREAM;
        try {
            if (blob != null) {
                is = blob.getPayload().openStream();
            }
        } catch (IOException cause) {
            throw new ObjectStoreException(
                "Failed to read blob " + blobId.asString(),
                cause);
        }
        return is;
    }
}
