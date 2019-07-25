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

package org.apache.dto;

import org.apache.james.json.DTOModule;

public interface TestModules {

    @SuppressWarnings("rawtypes")
    TestModule FIRST_TYPE = DTOModule
        .forDomainObject(FirstDomainObject.class)
        .convertToDTO(FirstDTO.class)
        .toDomainObjectConverter(FirstDTO::toDomainObject)
        .toDTOConverter((domainObject, typeName) -> new FirstDTO(
            typeName,
            domainObject.getId(),
            domainObject.getTime().toString(),
            domainObject.getPayload()))
        .typeName("first")
        .withFactory(TestModule::new);

    @SuppressWarnings("rawtypes")
    TestModule SECOND_TYPE = DTOModule
        .forDomainObject(SecondDomainObject.class)
        .convertToDTO(SecondDTO.class)
        .toDomainObjectConverter(SecondDTO::toDomainObject)
        .toDTOConverter((domainObject, typeName) -> new SecondDTO(
            typeName,
            domainObject.getId().toString(),
            domainObject.getPayload()))
        .typeName("second")
        .withFactory(TestModule::new);

}
