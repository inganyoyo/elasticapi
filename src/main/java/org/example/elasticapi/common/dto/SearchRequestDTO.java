package org.example.elasticapi.common.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchRequestDTO {

    private String indexName;
    private int page;
    private int size;
    private String keyword;
}
