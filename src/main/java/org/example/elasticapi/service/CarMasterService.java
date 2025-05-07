package org.example.elasticapi.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import lombok.RequiredArgsConstructor;
import org.example.elasticapi.dto.CarMaster;
import org.example.elasticapi.indexer.IndexerHelper;
import org.example.elasticapi.util.FileUtil;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class CarMasterService {
    private final IndexerHelper indexerHelper;
    public void createIndex(String indexName) throws IOException {
        Map<String, Object> indexTemplate = new FileUtil().getFileContent("/index/car-master.json");
//        Logger.getGlobal().info("indexTemplate : " + indexTemplate.get("mappings"));
        indexerHelper.createIndex(indexName, indexTemplate);
    }

    public void deleteIndex(String indexName) {
        indexerHelper.deleteIndex(indexName);
    }

    public Long countIndex(String indexName) {
        return indexerHelper.countIndex(indexName);
    }

    public List<CarMaster.Response> search(CarMaster.Request request) throws IOException {
        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(request.getIndexName())
                .query(q -> q.matchAll(m -> m))
                .size(100)
        );
        return indexerHelper.search(searchRequest, request.getIndexName());
    }
}