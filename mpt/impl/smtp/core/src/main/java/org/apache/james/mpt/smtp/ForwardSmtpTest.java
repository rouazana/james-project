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
package org.apache.james.mpt.smtp;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.james.mpt.api.SmtpHostSystem;
import org.apache.james.mpt.script.AbstractSimpleScriptedTestProtocol;
import org.apache.james.mpt.smtp.dns.InMemoryDNSService;
import org.apache.james.mpt.smtp.utils.DockerRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.ImmutableList;
import com.google.common.net.InetAddresses;

public class ForwardSmtpTest extends AbstractSimpleScriptedTestProtocol {

    public static final String USER = "bob@mydomain.tld";
    public static final String PASSWORD = "secret";

    private final TemporaryFolder folder = new TemporaryFolder();
    private final DockerRule fakeSmtp = new DockerRule("munkyboy/fakesmtp");

    @Rule
    public final RuleChain chain = RuleChain.outerRule(folder).around(fakeSmtp);

    @Inject
    private static SmtpHostSystem hostSystem;

    @Inject
    private static InMemoryDNSService dnsService;

    public ForwardSmtpTest() throws Exception {
        super(hostSystem, USER, PASSWORD, "/org/apache/james/smtp/scripts/");
    }

    @Before
    public void setUp() throws Exception {
        InetAddress containerIp = InetAddresses.forString(fakeSmtp.getContainerIp());
        dnsService.registerRecord("yopmail.com", new InetAddress[]{containerIp}, ImmutableList.of("yopmail.com"), ImmutableList.of());
        super.setUp();
    }

    @Test
    public void authenticateShouldWork() throws Exception {
        WatchService watcher = FileSystems.getDefault().newWatchService();
        WatchKey watchKey = Paths.get("/tmp/fakemail").register(watcher, StandardWatchEventKinds.ENTRY_CREATE);
        scriptTest("helo", Locale.US);
        WatchKey key = watcher.poll(20, TimeUnit.MINUTES);
        assertThat(key).isNotNull().isEqualTo(watchKey);
    }

}
