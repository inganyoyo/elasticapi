package org.example.elasticapi.core;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ElasticSearchClientManager {
    private final ElasticSearchFactory elasticSearchFactory;
    private final Map<String, ElasticsearchClient> clientMap = new ConcurrentHashMap<>();
    private final Map<String, RestClient> restClientMap = new ConcurrentHashMap<>();

    public ElasticsearchClient getClient(String indexName) {
        if (clientMap.containsKey(indexName)) {
            return clientMap.get(indexName);
        } else {
            ElasticsearchClient client = elasticSearchFactory.getElasticsearchClient();
            clientMap.put(indexName, client);
            return client;
        }
    }

    public RestClient getRestClient(String indexName) {
        if (restClientMap.containsKey(indexName)) {
            return restClientMap.get(indexName);
        } else {
            RestClient restClient = elasticSearchFactory.getRestClient();
            restClientMap.put(indexName, restClient);
            return restClient;
        }
    }
}
