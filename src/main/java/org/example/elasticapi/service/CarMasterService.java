package org.example.elasticapi.service;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import lombok.RequiredArgsConstructor;
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
}