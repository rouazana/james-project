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

package org.apache.james.mailbox.quota.cassandra;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.DockerCassandraExtension;
import org.apache.james.backends.cassandra.DockerCassandraExtension.DockerCassandra;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.mailbox.quota.QuotaThresholdHistoryStore;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class CassandraQuotaThresholdHistoryStoreExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {
    private final DockerCassandraExtension dockerCassandraExtension;
    private CassandraCluster cassandra;
    private DockerCassandra dockerCassandra;
    private CountChangesDao countChangesDao;
    private SizeChangesDao sizeChangesDao;

    public CassandraQuotaThresholdHistoryStoreExtension() {
        dockerCassandraExtension = new DockerCassandraExtension();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        dockerCassandraExtension.beforeAll(context);
        dockerCassandra = dockerCassandraExtension.getDockerCassandra();
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        dockerCassandraExtension.afterAll(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        cassandra = CassandraCluster.create(
                new CassandraQuotaThresholdHistoryStoreModule(), dockerCassandra.getIp(), dockerCassandra.getBindingPort());
        countChangesDao = new CountChangesDao(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
        sizeChangesDao = new SizeChangesDao(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        cassandra.close();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return (parameterContext.getParameter().getType() == QuotaThresholdHistoryStore.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return new CassandraQuotaThresholdHistoryStore(countChangesDao, sizeChangesDao);
    }
}
