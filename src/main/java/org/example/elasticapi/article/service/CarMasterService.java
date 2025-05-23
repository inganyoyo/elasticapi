package org.example.elasticapi.article.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.elasticapi.article.document.CarMaster;
import org.example.elasticapi.article.filter.FilterQueryEnum;
import org.example.elasticapi.core.ElasticSearchIndexerHelper;
import org.example.elasticapi.util.FileParser;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarMasterService {
    
    private final ElasticSearchIndexerHelper indexerHelper;
    private final FileParser fileParser;
    private final ObjectMapper objectMapper;

    public void createIndex(String indexName) throws IOException {
        String jsonString = fileParser.classpathJsonParser("/index/car-master.json");
        Map<String, Object> indexTemplate = objectMapper.readValue(jsonString, Map.class);
        indexerHelper.createIndex(indexName, indexTemplate);
    }

    public void deleteIndex(String indexName) {
        indexerHelper.deleteIndex(indexName);
    }

    public Long countIndex(String indexName) {
        return indexerHelper.countIndex(indexName);
    }

    private int getPageToFrom(int page, int size) {
        return (page - 1) * size;
    }

    public List<CarMaster.Response> search(CarMaster.Request request) throws IOException {
        int from = getPageToFrom(request.getPage(), request.getSize());

        // 쿼리 필드 리스트 미리 선언
        List<String> areaFields = List.of(
                "area.country.standard", "area.country.english", "area.country.korean", "area.country.combine",
                "area.region.standard", "area.region.english", "area.region.korean", "area.region.combine",
                "area.state.standard", "area.state.english", "area.state.korean", "area.state.combine"
        );

        List<String> generalFields = List.of(
                "brand.standard", "model.standard", "color.standard",
                "brand.english", "model.english", "color.english",
                "model.korean", "color.korean",
                "brand.combine", "model.combine", "color.combine"
        );

        List<String> includeFields = List.of(
                "image_url", "brand", "model", "price", "odometer", "year", "color"
        );

        // 필터 조건 구성
        BoolQuery.Builder filterBuilder = QueryBuilders.bool();
        for (FilterQueryEnum filterQueryEnum : FilterQueryEnum.values()) {
            filterQueryEnum.getQuery(filterBuilder, request);
        }

        // 검색 요청 빌드
        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(request.getIndexName())
                .source(ss -> ss.filter(sf -> sf.includes(includeFields)))
                .query(q -> q.bool(b -> b
                        .should(sh -> sh.nested(n -> n
                                .path("area")
                                .query(nq -> nq.multiMatch(mm -> mm
                                        .fields(areaFields)
                                        .query(request.getKeyword())
                                        .type(TextQueryType.CrossFields)
                                        .operator(Operator.And)
                                ))
                        ))
                        .should(sh -> sh.multiMatch(mm -> mm
                                .fields(generalFields)
                                .query(request.getKeyword())
                                .type(TextQueryType.CrossFields)
                                .operator(Operator.And)
                        ))
                        .minimumShouldMatch("1")
                        .filter(f -> f.bool(filterBuilder.build()))
                ))
                .from(from)
                .size(request.getSize())
                .sort(sort -> sort.field(f -> f.field("year").order(SortOrder.Desc)))
                .sort(sort -> sort.field(f -> f.field("price").order(SortOrder.Asc)))
                .sort(sort -> sort.field(f -> f.field("odometer").order(SortOrder.Asc)))
        );
        log.info("searchRequest : " + searchRequest.toString());

        return indexerHelper.search(request.getIndexName(), searchRequest, CarMaster.Response.class);
    }

    public void bulkInsertInBatches(String indexName, List<Map<String, String>> documents) {
        int batchSize = 1000;
        int total = documents.size();
        int fromIndex = 0;

        while (fromIndex < total) {
            int toIndex = Math.min(fromIndex + batchSize, total);
            List<Map<String, String>> batchList = documents.subList(fromIndex, toIndex);

            indexerHelper.bulkInsert(indexName, batchList);

            fromIndex = toIndex;
        }
    }

    public String parseDocument(String filePath) {
        File file = new File(filePath);
        return fileParser.parseFile(file);
    }
}