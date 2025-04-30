package org.example.elasticapi.controller;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.example.elasticapi.service.CarMasterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequiredArgsConstructor
public class CarMasterController {
    private final CarMasterService carMasterService;
    @GetMapping("/create-index")
    public void create(@RequestParam String indexName) throws IOException {
        carMasterService.createIndex(indexName);
    }

    @GetMapping("/delete-index")
    public void delete(@RequestParam String indexName) throws IOException {
        carMasterService.deleteIndex(indexName);
    }
}