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

package org.apache.james.jmap;

import static org.apache.james.jmap.BypassAuthOnRequestMethod.bypass;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.http.jetty.Configuration;
import org.apache.james.http.jetty.Configuration.Builder;
import org.apache.james.http.jetty.JettyHttpServer;
import org.apache.james.http.jetty.LambdaFilter;
import org.apache.james.lifecycle.api.Configurable;

import com.google.common.base.Throwables;


@Singleton
public class JMAPServer implements Configurable {

    private final JettyHttpServer server;

    @Inject
    private JMAPServer(PortConfiguration portConfiguration,
                       AuthenticationServlet authenticationServlet, JMAPServlet jmapServlet,
                       AuthenticationFilter authenticationFilter) {

        LambdaFilter provisionUserFilter = (req, resp, chain) -> chain.doFilter(req, resp);
        server = JettyHttpServer.create(
                configurationBuilderFor(portConfiguration)
                        .serve("/authentication")
                            .with(authenticationServlet)
                        .filter("/authentication")
                            .with(new AllowAllCrossOriginRequests(bypass(authenticationFilter).on("POST").and("OPTIONS").only()))
                            .only()
                        .serve("/jmap")
                            .with(jmapServlet)
                        .filter("/jmap")
                            .with(new AllowAllCrossOriginRequests(bypass(authenticationFilter).on("OPTIONS").only()))
                            .and(provisionUserFilter)
                            .only()
                        .build());
    }

    private Builder configurationBuilderFor(PortConfiguration portConfiguration) {
        Builder builder = Configuration.builder();
        if (portConfiguration.getPort().isPresent()) {
            builder.port(portConfiguration.getPort().get());
        } else {
            builder.randomPort();
        }
        return builder;
    }

    @Override
    public void configure(HierarchicalConfiguration config) throws ConfigurationException {
        try {
            server.start();
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    @PreDestroy
    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    public int getPort() {
        return server.getPort();
    }
}
