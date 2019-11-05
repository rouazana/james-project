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
package org.apache.james.rrt.lib;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.mail.internet.AddressException;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.james.core.Domain;
import org.apache.james.core.Username;
import org.apache.james.domainlist.api.DomainList;
import org.apache.james.domainlist.api.DomainListException;
import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.rrt.api.InvalidRegexException;
import org.apache.james.rrt.api.MappingAlreadyExistsException;
import org.apache.james.rrt.api.RecipientRewriteTable;
import org.apache.james.rrt.api.RecipientRewriteTableException;
import org.apache.james.rrt.api.SameSourceAndDestinationException;
import org.apache.james.rrt.api.SourceDomainIsNotInDomainListException;
import org.apache.james.rrt.lib.Mapping.Type;
import org.apache.james.util.OptionalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;

public abstract class AbstractRecipientRewriteTable implements RecipientRewriteTable, Configurable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRecipientRewriteTable.class);

    // The maximum mappings which will process before throwing exception
    private int mappingLimit = 10;

    private boolean recursive = true;

    private DomainList domainList;

    @Inject
    public void setDomainList(DomainList domainList) {
        this.domainList = domainList;
    }

    @Override
    public void configure(HierarchicalConfiguration<ImmutableNode> config) throws ConfigurationException {
        setRecursiveMapping(config.getBoolean("recursiveMapping", true));
        try {
            setMappingLimit(config.getInt("mappingLimit", 10));
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(e.getMessage());
        }
        doConfigure(config);
    }

    /**
     * Override to handle config
     */
    protected void doConfigure(HierarchicalConfiguration<ImmutableNode> conf) throws ConfigurationException {
    }

    public void setRecursiveMapping(boolean recursive) {
        this.recursive = recursive;
    }

    /**
     * Set the mappingLimit
     * 
     * @param mappingLimit
     *            the mappingLimit
     * @throws IllegalArgumentException
     *             get thrown if mappingLimit smaller then 1 is used
     */
    public void setMappingLimit(int mappingLimit) throws IllegalArgumentException {
        if (mappingLimit < 1) {
            throw new IllegalArgumentException("The minimum mappingLimit is 1");
        }
        this.mappingLimit = mappingLimit;
    }

    @Override
    public Mappings getResolvedMappings(String user, Domain domain) throws ErrorMappingException, RecipientRewriteTableException {
        return getMappings(Username.fromLocalPartWithDomain(user, domain), mappingLimit);
    }

    private Mappings getMappings(Username username, int mappingLimit) throws ErrorMappingException, RecipientRewriteTableException {

        // We have to much mappings throw ErrorMappingException to avoid
        // infinity loop
        if (mappingLimit == 0) {
            throw new TooManyMappingException("554 Too many mappings to process");
        }

        Mappings targetMappings = mapAddress(username.getLocalPart(), username.getDomainPart().get());


        try {
            return MappingsImpl.fromMappings(
                targetMappings.asStream()
                    .flatMap(Throwing.function((Mapping target) -> convertAndRecurseMapping(username, target, mappingLimit)).sneakyThrow()));
        } catch (SkipMappingProcessingException e) {
            return MappingsImpl.empty();
        }
    }

    private Stream<Mapping> convertAndRecurseMapping(Username originalUsername, Mapping associatedMapping, int remainingLoops) throws ErrorMappingException, RecipientRewriteTableException, SkipMappingProcessingException, AddressException {

        Function<Username, Stream<Mapping>> convertAndRecurseMapping =
            Throwing
                .function((Username rewrittenUser) -> convertAndRecurseMapping(associatedMapping, originalUsername, rewrittenUser, remainingLoops))
                .sneakyThrow();

        return associatedMapping.rewriteUser(originalUsername)
            .map(rewrittenUser -> rewrittenUser.withDefaultDomainFromUser(originalUsername))
            .map(convertAndRecurseMapping)
            .orElse(Stream.empty());
    }

    private Stream<Mapping> convertAndRecurseMapping(Mapping mapping, Username originalUsername, Username rewrittenUsername, int remainingLoops) throws ErrorMappingException, RecipientRewriteTableException {
        LOGGER.debug("Valid virtual user mapping {} to {}", originalUsername.asString(), rewrittenUsername.asString());

        Stream<Mapping> nonRecursiveResult = Stream.of(toMapping(rewrittenUsername, mapping.getType()));
        if (!recursive) {
            return nonRecursiveResult;
        }

        // Check if the returned mapping is the same as the input. If so we need to handle identity to avoid loops.
        if (originalUsername.equals(rewrittenUsername)) {
            return mapping.handleIdentity(nonRecursiveResult);
        } else {
            return recurseMapping(nonRecursiveResult, rewrittenUsername, remainingLoops);
        }
    }

    private Stream<Mapping> recurseMapping(Stream<Mapping> nonRecursiveResult, Username targetUsername, int remainingLoops) throws ErrorMappingException, RecipientRewriteTableException {
        Mappings childMappings = getMappings(targetUsername, remainingLoops - 1);

        if (childMappings.isEmpty()) {
            return nonRecursiveResult;
        } else {
            return childMappings.asStream();
        }
    }

    private Mapping toMapping(Username rewrittenUsername, Type type) {
        switch (type) {
            case Forward:
            case Group:
            case Alias:
                return Mapping.of(type, rewrittenUsername.asString());
            case Regex:
            case Domain:
            case Error:
            case Address:
                return Mapping.address(rewrittenUsername.asString());
        }
        throw new IllegalArgumentException("unhandled enum type");
    }

    @Override
    public void addRegexMapping(MappingSource source, String regex) throws RecipientRewriteTableException {
        try {
            Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new InvalidRegexException("Invalid regex: " + regex, e);
        }

        Mapping mapping = Mapping.regex(regex);
        checkDuplicateMapping(source, mapping);
        checkDomainMappingSourceIsManaged(source);
        LOGGER.info("Add regex mapping => {} for source {}", regex, source.asString());
        addMapping(source, mapping);
    }

    @Override
    public void removeRegexMapping(MappingSource source, String regex) throws RecipientRewriteTableException {
        LOGGER.info("Remove regex mapping => {} for source: {}", regex, source.asString());
        removeMapping(source, Mapping.regex(regex));
    }

    @Override
    public void addAddressMapping(MappingSource source, String address) throws RecipientRewriteTableException {
        Mapping mapping = Mapping.address(address)
            .appendDomainFromThrowingSupplierIfNone(this::defaultDomain);

        checkHasValidAddress(mapping);
        checkDuplicateMapping(source, mapping);
        checkDomainMappingSourceIsManaged(source);
        checkNotSameSourceAndDestination(source, address);

        LOGGER.info("Add address mapping => {} for source: {}", mapping.asString(), source.asString());
        addMapping(source, mapping);
    }

    private Domain defaultDomain() throws RecipientRewriteTableException {
        try {
            return domainList.getDefaultDomain();
        } catch (DomainListException e) {
            throw new RecipientRewriteTableException("Unable to retrieve default domain", e);
        }
    }

    private void checkHasValidAddress(Mapping mapping) throws RecipientRewriteTableException {
        if (!mapping.asMailAddress().isPresent()) {
            throw new RecipientRewriteTableException("Invalid emailAddress: " + mapping.asString());
        }
    }

    @Override
    public void removeAddressMapping(MappingSource source, String address) throws RecipientRewriteTableException {
        Mapping mapping = Mapping.address(address)
            .appendDomainFromThrowingSupplierIfNone(this::defaultDomain);

        LOGGER.info("Remove address mapping => {} for source: {}", mapping.asString(), source.asString());
        removeMapping(source, mapping);
    }

    @Override
    public void addErrorMapping(MappingSource source, String error) throws RecipientRewriteTableException {
        Mapping mapping = Mapping.error(error);

        checkDuplicateMapping(source, mapping);
        checkDomainMappingSourceIsManaged(source);

        LOGGER.info("Add error mapping => {} for source: {}", error, source.asString());
        addMapping(source, mapping);

    }

    @Override
    public void removeErrorMapping(MappingSource source, String error) throws RecipientRewriteTableException {
        LOGGER.info("Remove error mapping => {} for source: {}", error, source.asString());
        removeMapping(source, Mapping.error(error));
    }

    @Override
    public void addAliasDomainMapping(MappingSource source, Domain realDomain) throws RecipientRewriteTableException {
        checkDomainMappingSourceIsManaged(source);

        LOGGER.info("Add domain mapping: {} => {}", source.asDomain().map(Domain::asString).orElse("null"), realDomain);
        addMapping(source, Mapping.domain(realDomain));
    }

    @Override
    public void removeAliasDomainMapping(MappingSource source, Domain realDomain) throws RecipientRewriteTableException {
        LOGGER.info("Remove domain mapping: {} => {}", source.asDomain().map(Domain::asString).orElse("null"), realDomain);
        removeMapping(source, Mapping.domain(realDomain));
    }

    @Override
    public void addForwardMapping(MappingSource source, String address) throws RecipientRewriteTableException {
        Mapping mapping = Mapping.forward(address)
            .appendDomainFromThrowingSupplierIfNone(this::defaultDomain);

        checkHasValidAddress(mapping);
        checkDuplicateMapping(source, mapping);
        checkDomainMappingSourceIsManaged(source);

        LOGGER.info("Add forward mapping => {} for source: {}", mapping.asString(), source.asString());
        addMapping(source, mapping);
    }

    @Override
    public void removeForwardMapping(MappingSource source, String address) throws RecipientRewriteTableException {
        Mapping mapping = Mapping.forward(address)
            .appendDomainFromThrowingSupplierIfNone(this::defaultDomain);

        LOGGER.info("Remove forward mapping => {} for source: {}", mapping.asString(), source.asString());
        removeMapping(source, mapping);
    }

    @Override
    public void addGroupMapping(MappingSource source, String address) throws RecipientRewriteTableException {
        Mapping mapping = Mapping.group(address)
            .appendDomainFromThrowingSupplierIfNone(this::defaultDomain);

        checkHasValidAddress(mapping);
        checkDuplicateMapping(source, mapping);
        checkDomainMappingSourceIsManaged(source);

        LOGGER.info("Add group mapping => {} for source: {}", mapping.asString(), source.asString());
        addMapping(source, mapping);
    }

    @Override
    public void removeGroupMapping(MappingSource source, String address) throws RecipientRewriteTableException {
        Mapping mapping = Mapping.group(address)
            .appendDomainFromThrowingSupplierIfNone(this::defaultDomain);

        LOGGER.info("Remove group mapping => {} for source: {}", mapping.asString(), source.asString());
        removeMapping(source, mapping);
    }

    @Override
    public void addAliasMapping(MappingSource source, String address) throws RecipientRewriteTableException {
        Mapping mapping = Mapping.alias(address)
            .appendDomainFromThrowingSupplierIfNone(this::defaultDomain);

        checkHasValidAddress(mapping);
        checkDuplicateMapping(source, mapping);
        checkNotSameSourceAndDestination(source, address);
        checkDomainMappingSourceIsManaged(source);

        LOGGER.info("Add alias source => {} for destination mapping: {}", source.asString(), mapping.asString());
        addMapping(source, mapping);
    }

    @Override
    public void removeAliasMapping(MappingSource source, String address) throws RecipientRewriteTableException {
        Mapping mapping = Mapping.alias(address)
            .appendDomainFromThrowingSupplierIfNone(this::defaultDomain);

        LOGGER.info("Remove alias source => {} for destination mapping: {}", source.asString(), mapping.asString());
        removeMapping(source, mapping);
    }

    /**
     * Return a Map which holds all Mappings
     * 
     * @return Map
     */
    public abstract Map<MappingSource, Mappings> getAllMappings() throws RecipientRewriteTableException;

    /**
     * This method must return stored Mappings for the given user.
     * It must never return null but throw RecipientRewriteTableException on errors and return an empty Mappings
     * object if no mapping is found.
     */
    protected abstract Mappings mapAddress(String user, Domain domain) throws RecipientRewriteTableException;

    private void checkDomainMappingSourceIsManaged(MappingSource source) throws RecipientRewriteTableException {
        Optional<Domain> notManagedSourceDomain = OptionalUtils.toStream(source.availableDomain())
            .filter(Throwing.<Domain>predicate(domain -> !isManagedByDomainList(domain)).sneakyThrow())
            .findFirst();

        if (notManagedSourceDomain.isPresent()) {
            throw new SourceDomainIsNotInDomainListException("Source domain '" + notManagedSourceDomain.get().asString() + "' is not managed by the domainList");
        }
    }

    private boolean isManagedByDomainList(Domain domain) throws RecipientRewriteTableException {
        try {
            return domainList.containsDomain(domain);
        } catch (DomainListException e) {
            throw new RecipientRewriteTableException("Cannot verify domainList contains the available domain in source");
        }
    }

    private void checkDuplicateMapping(MappingSource source, Mapping mapping) throws RecipientRewriteTableException {
        Mappings mappings = getStoredMappings(source);
        if (mappings.contains(mapping)) {
            throw new MappingAlreadyExistsException("Mapping " + mapping.asString() + " for " + source.asString() + " already exist!");
        }
    }

    private void checkNotSameSourceAndDestination(MappingSource source, String address) throws RecipientRewriteTableException {
        if (source.asMailAddress().map(mailAddress -> mailAddress.asString().equals(address)).orElse(false)) {
            throw new SameSourceAndDestinationException("Source and destination can't be the same!");
        }
    }
}
