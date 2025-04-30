package org.example.elasticapi.indexer;

import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
import java.util.Map;
import java.util.logging.Logger;

@Component
@RequiredArgsConstructor
public class IndexerHelper {
    public final ElasticSearchClientManager elasticSearchClientManager;
    private final ObjectMapper objectMapper;

    public boolean createIndex(String indexName, Map<String, Object> indexTemplate) throws IOException {

//        elasticSearchClientManager.getClient(indexName)
//                .indices()
//                .create(c -> c.index(indexName));

        RestClient restClient = elasticSearchClientManager.getRestClient(indexName);
        Request request = new Request(HttpPut.METHOD_NAME, "/" + indexName);

        if(ObjectUtils.isNotEmpty(indexTemplate)){
            String requestBody = jsonMapToString(indexTemplate);
            HttpEntity httpEntity = new NStringEntity(requestBody, ContentType.create("application/json", StandardCharsets.UTF_8));

            request.setEntity(httpEntity);

            Response response = restClient.performRequest(request);
            Logger.getGlobal().info("response : " + response.toString());
        }
        return true ;
    }

    private String jsonMapToString(Map<String, Object> indexTemplate) throws JsonProcessingException {
        return objectMapper.writeValueAsString(indexTemplate );
    }

    public void deleteIndex(String indexName) {
        DeleteIndexRequest deleteIndexRequest = DeleteIndexRequest.of(d -> d.index(indexName));
        try {
            DeleteIndexResponse deleteIndexResponse = elasticSearchClientManager.getClient("indexer")
                    .indices()
                    .delete(deleteIndexRequest);

            Logger.getGlobal().info("deleteIndexResponse : " + deleteIndexResponse.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
