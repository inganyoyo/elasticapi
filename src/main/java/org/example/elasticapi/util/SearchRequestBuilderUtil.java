package org.example.elasticapi.util;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.msearch.MultisearchBody;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SearchRequestBuilderUtil {
    public static Query buildQuery(BoolQuery.Builder builder) {
        if (builder == null) {
            return null;
        }
        BoolQuery boolQuery = builder.build();

        // 조건이 하나라도 있을 경우에만 Query 생성
        Query query = null;
        if ((boolQuery.filter() != null && !boolQuery.filter().isEmpty()) ||
                (boolQuery.must() != null && !boolQuery.must().isEmpty()) ||
                (boolQuery.should() != null && !boolQuery.should().isEmpty()) ||
                (boolQuery.mustNot() != null && !boolQuery.mustNot().isEmpty())) {
            log.info("query : " + boolQuery.toString());
            query = Query.of(q -> q.bool(boolQuery));
        }
        return query;
    }

    public static SearchRequest buildSearchRequest(
            String indexName,
            List<String> includeFields,
            String keyword,
            List<NestedFieldGroup> nestedFieldGroups,
            List<FieldGroup> fieldGroups,
            Query filterQuery,
            List<SortField> sortFields,
            Integer from,
            Integer size
    ) {
        SearchRequest.Builder requestBuilder = new SearchRequest.Builder().index(indexName);

        // 1. Source filter
        if (includeFields != null && !includeFields.isEmpty()) {
            requestBuilder.source(src -> src.filter(f -> f.includes(includeFields)));
        }

        // 2. Query
        List<Query> shouldQueries = new ArrayList<>();

        // 2-1. Nested field group 검색
        if (nestedFieldGroups != null) {
            for (NestedFieldGroup group : nestedFieldGroups) {
                shouldQueries.add(Query.of(q -> q.nested(n -> n
                        .path(group.path())
                        .query(nq -> nq.multiMatch(m -> m
                                .query(keyword)
                                .fields(group.fields())
                                .fuzziness("AUTO")
                        ))
                )));
            }
        }

        // 2-2. 일반 field group 검색
        if (fieldGroups != null) {
            for (FieldGroup group : fieldGroups) {
                shouldQueries.add(Query.of(q -> q.multiMatch(m -> m
                        .query(keyword)
                        .fields(group.fields())
                        .fuzziness("AUTO")
                )));
            }
        }

        // 2-3. 최종 Bool 쿼리 구성
        requestBuilder.query(q -> q.bool(b -> {
            if (!shouldQueries.isEmpty()) {
                b.should(shouldQueries);
                b.minimumShouldMatch("1");
            }
            if (filterQuery != null) {
                b.filter(filterQuery);
            }
            return b;
        }));

        // 3. Sort
        if (sortFields != null && !sortFields.isEmpty()) {
            for (SortField sf : sortFields) {
                requestBuilder.sort(s -> s.field(f -> f.field(sf.field()).order(sf.order())));
            }
        }

        // 4. Paging
        if (from != null) {
            requestBuilder.from(from);
        }
        if (size != null) {
            requestBuilder.size(size);
        }

        // 5. Highlight
        if (keyword != null && !keyword.isEmpty()) {
            Highlight.Builder highlightBuilder = new Highlight.Builder();
            if (fieldGroups != null) {
                for (FieldGroup group : fieldGroups) {
                    for (String field : group.fields()) {
                        highlightBuilder.fields(field, f -> f);
                    }
                }
            }
            if (nestedFieldGroups != null) {
                for (NestedFieldGroup group : nestedFieldGroups) {
                    for (String field : group.fields()) {
                        highlightBuilder.fields(group.path() + "." + field, f -> f);
                    }
                }
            }
            requestBuilder.highlight(highlightBuilder.build());
        }

        // 6. Aggregation (예: count by field)
        requestBuilder.aggregations("count_by_field", a -> a
                .terms(t -> t.field("_index").size(10))
        );
        requestBuilder.trackScores(true);
        return requestBuilder.build();
    }

    /**
     * SearchRequest를 MultisearchBody로 변환합니다.
     * 단, Aggregations는 포함하지 않습니다 (멀티서치에서는 지원되지 않음).
     */
    public static MultisearchBody convertToMultiSearchBody(SearchRequest searchRequest) {

        if (searchRequest == null) {
            throw new IllegalArgumentException("searchRequest must not be null");
        }

        MultisearchBody.Builder builder = new MultisearchBody.Builder();

        builder
                .query(searchRequest.query()) // null-safe
                .from(searchRequest.from())
                .size(searchRequest.size())
                .highlight(searchRequest.highlight());

        if (searchRequest.sort() != null && !searchRequest.sort().isEmpty()) {
            builder.sort(searchRequest.sort());
        }

        if (searchRequest.source() != null) {
            builder.source(searchRequest.source());
        }

        return builder.build();
    }

    public record NestedFieldGroup(String path, List<String> fields) {
    }

    public record FieldGroup(List<String> fields) {
    }

    public record SortField(String field, SortOrder order) {
    }

}

