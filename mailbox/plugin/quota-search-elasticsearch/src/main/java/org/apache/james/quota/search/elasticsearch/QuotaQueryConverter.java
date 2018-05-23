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

package org.apache.james.quota.search.elasticsearch;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.apache.james.quota.search.elasticsearch.json.JsonMessageConstants.DOMAIN;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.james.quota.search.QuotaClause;
import org.apache.james.quota.search.QuotaQuery;
import org.apache.james.quota.search.QuotaClause.And;
import org.apache.james.quota.search.QuotaClause.HasDomain;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public class QuotaQueryConverter {
    private final Map<Class<? extends QuotaClause>, Function<QuotaClause, QueryBuilder>> clauseConverter;

    public QuotaQueryConverter() {
        Builder<Class<? extends QuotaClause>, Function<QuotaClause, QueryBuilder>> builder = ImmutableMap.builder();
        builder.put(HasDomain.class, clause -> termQuery(DOMAIN, ((HasDomain)clause).getDomain().asString()));
        builder.put(And.class, any -> { throw new RuntimeException("Nested and clauses are not supported"); });
        clauseConverter = builder.build();
    }

    public QueryBuilder from(QuotaQuery query) {
        List<QuotaClause> clauses = query.getClause().getClauses();
        if (clauses.isEmpty()) {
            return matchAllQuery();
        }
        if (clauses.size() == 1) {
            return toElasticSearch(clauses.get(0));
        }
        
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        clauses.stream()
            .map(this::toElasticSearch)
            .forEach(boolQueryBuilder::must);
        return boolQueryBuilder;
    }

    private QueryBuilder toElasticSearch(QuotaClause clause) {
        return clauseConverter.get(clause.getClass()).apply(clause);
    }

}
