package org.example.elasticapi.core;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticSearchIndexerHelper {
    public final ElasticSearchClientManager elasticSearchClientManager;
    private final ObjectMapper objectMapper;

    public boolean createIndex(String indexName, Map<String, Object> indexTemplate) throws IOException {

        RestClient restClient = elasticSearchClientManager.getRestClient(indexName);
        Request request = new Request(HttpPut.METHOD_NAME, "/" + indexName);

        if (ObjectUtils.isNotEmpty(indexTemplate)) {
            String requestBody = objectMapper.writeValueAsString(indexTemplate);
            HttpEntity httpEntity = new NStringEntity(requestBody, ContentType.create("application/json", StandardCharsets.UTF_8));

            request.setEntity(httpEntity);

            Response response = restClient.performRequest(request);
            log.info("response : " + response.toString());
        }
        return true;
    }


    public void deleteIndex(String indexName) {
        DeleteIndexRequest deleteIndexRequest = DeleteIndexRequest.of(d -> d.index(indexName));
        try {
            DeleteIndexResponse deleteIndexResponse = elasticSearchClientManager.getClient(indexName)
                    .indices()
                    .delete(deleteIndexRequest);

            log.info("deleteIndexResponse : " + deleteIndexResponse.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Long countIndex(String indexName) {
        CountRequest countRequest = CountRequest.of(c -> c.index(indexName));
        try {
            return elasticSearchClientManager.getClient(indexName)
                    .count(countRequest)
                    .count();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void bulkInsert(String indexName, List<Map<String, String>> documents) {
        ElasticsearchClient client = elasticSearchClientManager.getClient(indexName);

        try {
            BulkRequest.Builder br = new BulkRequest.Builder();

            for (Map<String, String> docMap : documents) {
                String id = docMap.get("id");
                docMap.remove("id");
                br.operations(op -> op
                        .index(idx -> idx
                                .index(indexName)
                                .id(id)
                                .document(docMap) // 전체 객체가 아닌 필요한 필드만
                        )
                );
            }

            BulkResponse response = client.bulk(br.build());
            if (response.errors()) {
                log.error("Bulk insert error : " + response.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 공통 검색 메서드 (Map 형태로 반환)
     */
    public List<Map<String, Object>> search(String indexName, SearchRequest searchRequest) throws IOException {
        ElasticsearchClient client = elasticSearchClientManager.getClient(indexName);

        SearchResponse<Map> response = client.search(searchRequest, Map.class);
        return response.hits().hits().stream()
                .map(Hit::source)
                .map(source -> (Map<String, Object>) source)
                .collect(Collectors.toList());
    }

}
