package org.example.elasticapi.article.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.elasticapi.article.document.CarMaster;
import org.example.elasticapi.article.service.CarMasterService;
import org.example.elasticapi.util.FileParser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CarMasterController {
    private final CarMasterService carMasterService;
    private final FileParser fileParser;
    private final ObjectMapper objectMapper;

    @GetMapping("/create-index")
    public void create(@RequestParam String indexName) throws IOException {
        carMasterService.createIndex(indexName);
    }

    @GetMapping("/delete-index")
    public void delete(@RequestParam String indexName) throws IOException {
        carMasterService.deleteIndex(indexName);
    }

    @GetMapping("/count-index")
    public Long count(@RequestParam String indexName) throws IOException {
        return carMasterService.countIndex(indexName);
    }

    @GetMapping("/search")
    public List<CarMaster.Response> search(@ModelAttribute CarMaster.Request request) throws IOException {
        List<CarMaster.Response> responseList = carMasterService.search(request);
        log.info("responseList : " + objectMapper.writeValueAsString(responseList));
        return responseList;
    }


    @GetMapping("/bulkInsert")
    public void bulkInsert() throws IOException {
        log.info(fileParser.classpathJsonParser("/index/car-master.json"));
        log.info("bulkInsert");
        int totalCount = 10000;
        List<Map<String, String>> documents = new ArrayList<>(totalCount);
        for (int i = 1; i <= totalCount; i++) {
            Map<String, String> docMap = new HashMap<String, String>();
            docMap.put("id", UUID.randomUUID().toString());
            docMap.put("title", "Title " + i);
            docMap.put("content", "This is the content for article number " + i + ".");
            docMap.put("attachments_content", "Attachment content example for article " + i);

            documents.add(docMap);
        }

        carMasterService.bulkInsertInBatches("search-article-index", documents);
    }

    @GetMapping("/parseFile")
    public void parseFile(@RequestParam String file) throws IOException {
        log.info("file : " + file);
        String contents = carMasterService.parseDocument(file);
        log.info(contents);
    }
}
