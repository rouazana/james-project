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

import java.net.URI;
import java.util.Optional;
import java.util.Properties;

import org.jclouds.openstack.keystone.config.KeystoneProperties;
import org.jclouds.openstack.swift.v1.reference.TempAuthHeaders;

public class CloudsBlobsConfiguration {
    private final URI endpoint;
    private final Optional<Region> region;
    private final Identity identity;
    private final Properties overrides;
    private final Credentials credentials;

    public static class Builder {
        private URI endpoint;
        private Identity identity;
        private Credentials credentials;
        private Optional<Region> region;
        private final Properties overrides;

        public Builder() {
            region = Optional.empty();
            this.overrides = new Properties();
        }

        public CloudsBlobsConfiguration.Builder withEndpoint(URI endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public CloudsBlobsConfiguration.Builder withIdentity(Identity identity) {
            this.identity = identity;
            return this;
        }

        public CloudsBlobsConfiguration.Builder withCredentials(Credentials credentials) {
            this.credentials = credentials;
            return this;
        }

        public CloudsBlobsConfiguration.Builder withRegion(Region region) {
            this.region = Optional.of(region);
            return this;
        }

        public CloudsBlobsConfiguration.Builder withTempAuthHeaderUserName(String tmpAuthHeaderUser) {
            this.overrides.setProperty(TempAuthHeaders.TEMP_AUTH_HEADER_USER, tmpAuthHeaderUser);
            return this;
        }

        public CloudsBlobsConfiguration.Builder withTempAuthHeaderPassName(String tmpAuthHeaderUser) {
            this.overrides.setProperty(TempAuthHeaders.TEMP_AUTH_HEADER_PASS, tmpAuthHeaderUser);
            return this;
        }

        public CloudsBlobsConfiguration build() {
            overrides.setProperty(KeystoneProperties.CREDENTIAL_TYPE, "tempAuthCredentials");
            return new CloudsBlobsConfiguration(endpoint, identity, credentials, region, overrides);
        }
    }

    CloudsBlobsConfiguration(URI endpoint,
                             Identity identity,
                             Credentials credentials,
                             Optional<Region> region,
                             Properties overrides) {
        this.endpoint = endpoint;
        this.region = region;
        this.overrides = overrides;
        this.identity = identity;
        this.credentials = credentials;
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public Identity getIdentity() {
        return identity;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public Properties getOverrides() {
        return overrides;
    }

    public Optional<Region> getRegion() {
        return region;
    }
}
