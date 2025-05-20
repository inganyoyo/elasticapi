package org.example.elasticapi.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import lombok.RequiredArgsConstructor;
import org.example.elasticapi.construct.FilterQueryEnum;
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

    private int getPageToFrom(int page, int size) {
        return (page - 1) * size;
    }

    public List<CarMaster.Response> search(CarMaster.Request request) throws IOException {
        //page 계산
        int from = getPageToFrom(request.getPage(), request.getSize());
        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(request.getIndexName())
                .source(ss -> ss.filter(sf -> sf
                        .includes("image_url",
                                "brand",
                                "model",
                                "price",
                                "odometer",
                                "year",
                                "color"))
                )
                .query(q -> q
                        .bool(b -> b
                                .should(sh -> sh
                                        .nested(shn -> shn
                                                .path("area")
                                                .query(shnq -> shnq
                                                        .multiMatch(shnqm -> shnqm
                                                                .fields(List.of("area.country.standard",
                                                                        "area.country.english",
                                                                        "area.country.korean",
                                                                        "area.country.combine",
                                                                        "area.region.standard",
                                                                        "area.region.english",
                                                                        "area.region.korean",
                                                                        "area.region.combine",
                                                                        "area.state.standard",
                                                                        "area.state.english",
                                                                        "area.state.korean",
                                                                        "area.state.combine"))
                                                                .query(request.getKeyword())
                                                                .type(TextQueryType.CrossFields)
                                                                .operator(Operator.And)
                                                        )
                                                )
                                        )
                                )
                                .should(sh -> sh
                                        .multiMatch(shnqm -> shnqm
                                                .fields(List.of(
                                                        "brand.standard",
                                                        "model.standard",
                                                        "color.standard",
                                                        "brand.english",
                                                        "model.english",
                                                        "color.english",
                                                        "model.korean",
                                                        "color.korean",
                                                        "brand.combine",
                                                        "model.combine",
                                                        "color.combine"))
                                                .query(request.getKeyword())
                                                .type(TextQueryType.CrossFields)
                                                .operator(Operator.And)
                                        )
                                )
                                .minimumShouldMatch("1")
                                .filter(sf -> {
                                    BoolQuery.Builder boolQueryBuilder = QueryBuilders.bool();
                                    for (FilterQueryEnum filterQueryEnum : FilterQueryEnum.values()) {
                                        filterQueryEnum.getQuery(boolQueryBuilder, request);
                                    }
                                    sf.bool(boolQueryBuilder.build());
                                    return sf;
                                })
                        )
                )
                .from(from)
                .size(request.getSize())
                .sort(so -> so
                        .field(FieldSort.of(sof -> sof
                                        .field("year")
                                        .order(SortOrder.Desc)
                                )

                        )
                )
                .sort(so -> so
                        .field(FieldSort.of(sof -> sof
                                        .field("price")
                                        .order(SortOrder.Asc)
                                )

                        )
                )
                .sort(so -> so
                        .field(FieldSort.of(sof -> sof
                                        .field("odometer")
                                        .order(SortOrder.Asc)
                                )

                        )
                )
        );

        return indexerHelper.search(searchRequest, request.getIndexName());
    }

    public List<CarMaster.CompletionResponse> completion(CarMaster.CompletionRequest request) throws IOException {
        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(request.getIndexName())
                .query(q -> q
                        .bool(b -> b
                                .minimumShouldMatch("1")
                                .should(sh -> sh
                                        .multiMatch(shm -> shm
                                                .query(request.getKeyword())
                                                .fields(List.of("brand.completion_chosung",
                                                        "brand.completion_eng2kor",
                                                        "brand.completion_jamo",
                                                        "brand.completion_kor2eng",
                                                        "color.completion_chosung",
                                                        "color.completion_eng2kor",
                                                        "color.completion_jamo",
                                                        "color.completion_kor2eng",
                                                        "drive.completion_chosung",
                                                        "drive.completion_eng2kor",
                                                        "drive.completion_jamo",
                                                        "drive.completion_kor2eng",
                                                        "manufacturer.completion_chosung",
                                                        "manufacturer.completion_eng2kor",
                                                        "manufacturer.completion_jamo",
                                                        "manufacturer.completion_kor2eng",
                                                        "model.completion_chosung",
                                                        "model.completion_eng2kor",
                                                        "model.completion_jamo",
                                                        "model.completion_kor2eng",
                                                        "size.completion_chosung",
                                                        "size.completion_eng2kor",
                                                        "size.completion_jamo",
                                                        "size.completion_kor2eng",
                                                        "type.completion_chosung",
                                                        "type.completion_eng2kor",
                                                        "type.completion_jamo",
                                                        "type.completion_kor2eng",
                                                        "type.completion_chosung",
                                                        "type.completion_eng2kor",
                                                        "type.completion_jamo",
                                                        "type.completion_kor2eng")
                                                )
                                                .operator(Operator.And)
                                                .type(TextQueryType.CrossFields)
                                        )
                                )
                                .should(sh -> sh
                                        .nested(shn -> shn
                                                .path("area")
                                                .query(shnq -> shnq
                                                        .multiMatch(shnqm -> shnqm
                                                                .query(request.getKeyword())
                                                                .fields(List.of("area.country.completion_chosung",
                                                                        "area.country.completion_eng2kor",
                                                                        "area.country.completion_jamo",
                                                                        "area.country.completion_kor2eng",
                                                                        "area.region.completion_chosung",
                                                                        "area.region.completion_eng2kor",
                                                                        "area.region.completion_jamo",
                                                                        "area.region.completion_kor2eng",
                                                                        "area.state.completion_chosung",
                                                                        "area.state.completion_eng2kor",
                                                                        "area.state.completion_jamo",
                                                                        "area.state.completion_kor2eng")
                                                                )
                                                                .operator(Operator.And)
                                                                .type(TextQueryType.CrossFields)
                                                        )

                                                )

                                        )
                                )
                        ))
                .sort(so -> so
                        .field(FieldSort.of(sof -> sof
                                        .field("odometer")
                                        .order(SortOrder.Asc)
                                )
                        )
                )
                .source(sou -> sou
                        .filter(souf -> souf
                                .includes(List.of("id", "year", "manufacturer", "transmission", "model", "brand"))
                        )
                )
                .size(10)
        );
        return indexerHelper.completionSearch(searchRequest, request.getIndexName());
    }
}