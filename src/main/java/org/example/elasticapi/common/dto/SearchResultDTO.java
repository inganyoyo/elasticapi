package org.example.elasticapi.common.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SearchResultDTO {
    private String indexName;
    private long total;
    private long took;
    private double maxScore;
    private int page;
    private int size;
    private String scrollId;
    private List<Map<String, Object>> results;
    private List<List<Object>> sortValuesList;
    private Map<String, Object> aggregations;
    private List<Map<String, List<String>>> highlights;
    private String errorMessage;


    public SearchResultDTO(String indexName, long total, long took, double maxScore, int page, int size,
                           String scrollId, List<Map<String, Object>> results,
                           List<List<Object>> sortValuesList, Map<String, Object> aggregations,
                           List<Map<String, List<String>>> highlights, String errorMessage) {
        this.indexName = indexName;
        this.total = total;
        this.took = took;
        this.maxScore = maxScore;
        this.page = page;
        this.size = size;
        this.scrollId = scrollId;
        this.results = results;
        this.sortValuesList = sortValuesList;
        this.aggregations = aggregations;
        this.highlights = highlights;
        this.errorMessage = errorMessage;
    }
    // getter, setter 생략
}
