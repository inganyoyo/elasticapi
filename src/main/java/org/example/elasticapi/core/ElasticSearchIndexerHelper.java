package org.example.elasticapi.core;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchItem;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchResponseItem;
import co.elastic.clients.elasticsearch.core.msearch.MultisearchBody;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
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
import org.example.elasticapi.common.dto.SearchResultDTO;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
     * Performs a search query on the specified Elasticsearch index and returns the results.
     * The results include hits, sort values (extracted from FieldValue), aggregations, and highlights.
     *
     * @param clientName    the name of the Elasticsearch index to search
     * @param searchRequest the search request containing query parameters
     * @return a {@link SearchResultDTO} containing search results, metadata, aggregations, and highlights
     * @throws IllegalArgumentException if indexName or searchRequest is null or empty
     * @throws IOException              if an error occurs during the search operation
     */
    public SearchResultDTO search(String clientName, SearchRequest searchRequest) throws IOException {
        // Input validation
        if (clientName == null || clientName.trim().isEmpty()) {
            throw new IllegalArgumentException("indexName must not be null or empty");
        }
        if (searchRequest == null) {
            throw new IllegalArgumentException("searchRequest must not be null");
        }

        // Execute search
        ElasticsearchClient client = elasticSearchClientManager.getClient(clientName);
        SearchResponse<Map> response;
        try {
            response = client.search(searchRequest, Map.class);
        } catch (Exception e) {
            log.error("Search failed for clientName {}: {}", clientName, e.getMessage());
            throw new IOException("Failed to execute search on clientName: " + clientName, e);
        }

        // Extract metadata
        String indexName = extractIndexName(response);
        long total = extractTotalHits(response);
        long took = extractTook(response);
        double maxScore = extractMaxScore(response);
        int size = extractSize(searchRequest);
        int page = calculatePage(searchRequest, size);
        String scrollId = response.scrollId();

        // Extract results, sort values, aggregations, and highlights
        List<Map<String, Object>> results = extractResults(response);
        List<List<Object>> sortValuesList = extractSortValues(response);
        Map<String, Object> aggregations = extractAggregations(response);
        List<Map<String, List<String>>> highlights = extractHighlights(response);

        return new SearchResultDTO(
                indexName, total, took, maxScore, page, size, scrollId,
                results, sortValuesList, aggregations, highlights, ""
        );
    }


    /**
     * Performs a multi-search query on the specified Elasticsearch index and returns a list of results.
     * Each search request in the multi-search request is executed, and results include hits, sort values,
     * aggregations, and highlights for each individual search.
     *
     * @param clientName     the name of the Elasticsearch index to search
     * @param msearchRequest the multi-search request containing multiple search queries
     * @return a {@link List<SearchResultDTO>} containing results for each search query
     * @throws IllegalArgumentException if indexName or msearchRequest is null or empty
     * @throws IOException              if an error occurs during the multi-search operation
     */
    public List<SearchResultDTO> multiSearch(String clientName, MsearchRequest msearchRequest) throws IOException {
        // Input validation
        if (clientName == null || clientName.trim().isEmpty()) {
            throw new IllegalArgumentException("indexName must not be null or empty");
        }
        if (msearchRequest == null || msearchRequest.searches().isEmpty()) {
            throw new IllegalArgumentException("msearchRequest must not be null or empty");
        }

        // Execute multi-search
        ElasticsearchClient client = elasticSearchClientManager.getClient(clientName);
        MsearchResponse<Map> multiResponse;
        try {
            multiResponse = client.msearch(msearchRequest, Map.class);
        } catch (Exception e) {
            log.error("Multi-search failed for clientName {}: {}", clientName, e.getMessage());
            throw new IOException("Failed to execute multi-search on clientName: " + clientName, e);
        }

        // Process each response
        List<SearchResultDTO> results = new ArrayList<>();
        for (int i = 0; i < multiResponse.responses().size(); i++) {
            MultiSearchResponseItem<Map> item = multiResponse.responses().get(i);
            SearchResultDTO resultDTO;

            if (item.isFailure()) {
                // Handle individual search failure
                String errorMessage = item.failure() != null ? item.failure().error().reason() : "Unknown error";
                log.warn("Individual search failed in multi-search for clientName {}: {}", clientName, errorMessage);
                resultDTO = createErrorResult(errorMessage);
            } else {
                // 성공적인 검색 응답 처리
//                SearchResponse<Map> response = item.result();
                MultiSearchItem<Map> response = item.result();
                msearchRequest.searches().get(i).body();


                MultisearchBody multisearchBody = msearchRequest.searches().get(i).body();

                // Extract metadata
                String indexName = extractIndexName(response);
                long total = extractTotalHits(response);
                long took = extractTook(response);
                double maxScore = extractMaxScore(response);
                int size = extractSize(multisearchBody);
                int page = calculatePage(multisearchBody, size);
                String scrollId = response.scrollId();

                // Extract results, sort values, aggregations, and highlights
                List<Map<String, Object>> searchResults = extractResults(response);
                List<List<Object>> sortValuesList = extractSortValues(response);
                Map<String, Object> aggregations = extractAggregations(response);
                List<Map<String, List<String>>> highlights = extractHighlights(response);

                resultDTO = new SearchResultDTO(
                        indexName, total, took, maxScore, page, size, scrollId,
                        searchResults, sortValuesList, aggregations, highlights, ""
                );
            }

            results.add(resultDTO);
        }

        return results;
    }

    /**
     * Creates a SearchResultDTO for a failed search with an error message.
     *
     * @param errorMessage the error message describing the failure
     * @return a {@link SearchResultDTO} with error details
     */
    private SearchResultDTO createErrorResult(String errorMessage) {
        return new SearchResultDTO(
                null,
                0L, 0L, 0.0, 1, 0, null,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap(),
                Collections.emptyList(),
                errorMessage
        );
    }

    /**
     * Extracts the index name from the first hit in the search response.
     * Returns null if no hits are present.
     *
     * @param response the search response containing hit metadata
     * @return the index name of the first hit, or null if unavailable
     */
    private String extractIndexName(SearchResponse<Map> response) {
        return Optional.ofNullable(response.hits())
                .map(HitsMetadata::hits)
                .filter(hits -> !hits.isEmpty())
                .map(hits -> hits.get(0).index())
                .orElse(null);
    }

    /**
     * Extracts the total number of hits from the search response.
     * Returns 0 if no total hits are available.
     *
     * @param response the search response containing hit metadata
     * @return the total number of hits
     */
    private long extractTotalHits(SearchResponse<Map> response) {
        return Optional.ofNullable(response.hits())
                .map(HitsMetadata::total)
                .map(TotalHits::value)
                .orElse(0L);
    }

    /**
     * Extracts the time taken for the search operation in milliseconds.
     * Returns 0 if the took value is unavailable.
     *
     * @param response the search response containing timing information
     * @return the time taken for the search in milliseconds
     */
    private long extractTook(SearchResponse<Map> response) {
        return Optional.ofNullable(response.took()).orElse(0L);
    }

    /**
     * Extracts the maximum score from the search response.
     * Returns 0.0 if no maximum score is available.
     *
     * @param response the search response containing hit metadata
     * @return the maximum score of the hits
     */
    private double extractMaxScore(SearchResponse<Map> response) {
        return Optional.ofNullable(response.hits())
                .map(HitsMetadata::maxScore)
                .orElse(0.0);
    }

    /**
     * Calculates the current page number based on the 'from' offset and page size.
     * Returns 1 if the size is 0 or invalid.
     *
     * @param searchRequest the search request containing pagination parameters
     * @param size          the number of results per page
     * @return the current page number
     */
    private int calculatePage(SearchRequest searchRequest, int size) {
        return size > 0 ? (searchRequest.from() / size) + 1 : 1;
    }

    /**
     * Extracts the page size (number of results per page) from the search request.
     * Returns 10 if the size is not specified.
     *
     * @param searchRequest the search request containing pagination parameters
     * @return the page size
     */
    private int extractSize(SearchRequest searchRequest) {
        return searchRequest.size() != null ? searchRequest.size() : 10;
    }

    /**
     * Extracts search results, including source data and metadata (_index, _id, _score).
     * Returns a list of maps, each containing the hit’s source and metadata.
     *
     * @param response the search response containing hit data
     * @return a list of maps with source data and metadata
     */
    private List<Map<String, Object>> extractResults(SearchResponse<Map> response) {
        return response.hits().hits().stream()
                .map(hit -> {
                    Map<String, Object> sourceMap = new HashMap<>();
                    Map<String, Object> src = hit.source();
                    if (src != null) {
                        sourceMap.putAll(src);
                    }
                    sourceMap.put("_index", hit.index());
                    sourceMap.put("_id", hit.id());
                    sourceMap.put("_score", hit.score());
                    return sourceMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * Extracts sort values from the search response, converting them to raw values.
     * Returns a list of lists, where each inner list contains sort values for a hit.
     *
     * @param response the search response containing hit sort values
     * @return a list of sort value lists for each hit
     */
    private List<List<Object>> extractSortValues(SearchResponse<Map> response) {
        return response.hits().hits().stream()
                .map(hit -> {
                    List<?> sortValues = hit.sort();
                    if (sortValues == null) {
                        return Collections.<Object>emptyList();
                    }
                    return sortValues.stream()
                            .map(o -> (Object) o)
                            .collect(Collectors.toList());
                })
                .collect(Collectors.toList());
    }

    /**
     * Extracts aggregations from the search response.
     * Returns a map of aggregation names to their values, or an empty map if none exist.
     *
     * @param response the search response containing aggregation data
     * @return a map of aggregation names to values
     */
    private Map<String, Object> extractAggregations(SearchResponse<Map> response) {
        Map<String, Aggregate> aggregations = response.aggregations();
        if (aggregations == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> resultMap = new HashMap<>();
        for (Map.Entry<String, Aggregate> entry : aggregations.entrySet()) {
            resultMap.put(entry.getKey(), entry.getValue());
        }
        return resultMap;
    }

    /**
     * Extracts highlights from the search response.
     * Returns a list of highlight maps, where each map contains field names and their highlighted fragments.
     *
     * @param response the search response containing highlight data
     * @return a list of highlight maps for each hit
     */
    private List<Map<String, List<String>>> extractHighlights(SearchResponse<Map> response) {
        return Optional.ofNullable(response.hits())
                .map(HitsMetadata::hits)
                .orElseGet(Collections::emptyList)
                .stream()
                .map(hit -> Optional.ofNullable(hit.highlight())
                        .orElseGet(Collections::emptyMap))
                .collect(Collectors.toList());
    }

    /**
     * Extracts the index name from the first hit in the multi-search item response.
     * Returns null if no hits are present.
     *
     * @param response the multi-search item response containing hit metadata
     * @return the index name of the first hit, or null if unavailable
     */
    private String extractIndexName(MultiSearchItem<Map> response) {
        return Optional.ofNullable(response.hits())
                .map(HitsMetadata::hits)
                .filter(hits -> !hits.isEmpty())
                .map(hits -> hits.get(0).index())
                .orElse(null);
    }

    /**
     * Extracts the total number of hits from the multi-search item response.
     * Returns 0 if no total hits are available.
     *
     * @param response the multi-search item response containing hit metadata
     * @return the total number of hits
     */
    private long extractTotalHits(MultiSearchItem<Map> response) {
        return Optional.ofNullable(response.hits())
                .map(HitsMetadata::total)
                .map(TotalHits::value)
                .orElse(0L);
    }

    /**
     * Extracts the time taken for the multi-search operation in milliseconds.
     * Returns 0 if the took value is unavailable.
     *
     * @param response the multi-search item response containing timing information
     * @return the time taken for the search in milliseconds
     */
    private long extractTook(MultiSearchItem<Map> response) {
        return Optional.ofNullable(response.took()).orElse(0L);
    }

    /**
     * Extracts the maximum score from the multi-search item response.
     * Returns 0.0 if no maximum score is available.
     *
     * @param response the multi-search item response containing hit metadata
     * @return the maximum score of the hits
     */
    private double extractMaxScore(MultiSearchItem<Map> response) {
        return Optional.ofNullable(response.hits())
                .map(HitsMetadata::maxScore)
                .orElse(0.0);
    }

    /**
     * Calculates the current page number based on the 'from' offset and page size.
     * Returns 1 if the size is 0 or invalid.
     *
     * @param multisearchBody the multi-search body containing pagination parameters
     * @param size            the number of results per page
     * @return the current page number
     */
    private int calculatePage(MultisearchBody multisearchBody, int size) {
        return size > 0 ? (multisearchBody.from() / size) + 1 : 1;
    }

    /**
     * Extracts the page size (number of results per page) from the multi-search body.
     * Returns 10 if the size is not specified.
     *
     * @param multisearchBody the multi-search body containing pagination parameters
     * @return the page size
     */
    private int extractSize(MultisearchBody multisearchBody) {
        return multisearchBody.size() != null ? multisearchBody.size() : 10;
    }

    /**
     * Extracts search results from the multi-search item, including source data and metadata (_index, _id, _score).
     * Returns a list of maps, each containing the hit’s source and metadata.
     *
     * @param response the multi-search item response containing hit data
     * @return a list of maps with source data and metadata
     */
    private List<Map<String, Object>> extractResults(MultiSearchItem<Map> response) {
        return response.hits().hits().stream()
                .map(hit -> {
                    Map<String, Object> sourceMap = new HashMap<>();
                    Map<String, Object> src = hit.source();
                    if (src != null) {
                        sourceMap.putAll(src);
                    }
                    sourceMap.put("_index", hit.index());
                    sourceMap.put("_id", hit.id());
                    sourceMap.put("_score", hit.score());
                    return sourceMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * Extracts sort values from the multi-search item response, converting them to raw values.
     * Returns a list of lists, where each inner list contains sort values for a hit.
     *
     * @param response the multi-search item response containing hit sort values
     * @return a list of sort value lists for each hit
     */
    private List<List<Object>> extractSortValues(MultiSearchItem<Map> response) {
        return response.hits().hits().stream()
                .map(hit -> {
                    List<?> sortValues = hit.sort();
                    if (sortValues == null) {
                        return Collections.<Object>emptyList();
                    }
                    return sortValues.stream()
                            .map(o -> (Object) o)
                            .collect(Collectors.toList());
                })
                .collect(Collectors.toList());
    }

    /**
     * Extracts aggregations from the multi-search item response.
     * Returns a map of aggregation names to their values, or an empty map if none exist.
     *
     * @param response the multi-search item response containing aggregation data
     * @return a map of aggregation names to values
     */
    private Map<String, Object> extractAggregations(MultiSearchItem<Map> response) {
        Map<String, Aggregate> aggregations = response.aggregations();
        if (aggregations == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> resultMap = new HashMap<>();
        for (Map.Entry<String, Aggregate> entry : aggregations.entrySet()) {
            resultMap.put(entry.getKey(), entry.getValue());
        }
        return resultMap;
    }

    /**
     * Extracts highlights from the multi-search item response.
     * Returns a list of highlight maps, where each map contains field names and their highlighted fragments.
     *
     * @param response the multi-search item response containing highlight data
     * @return a list of highlight maps for each hit
     */
    private List<Map<String, List<String>>> extractHighlights(MultiSearchItem<Map> response) {
        return Optional.ofNullable(response.hits())
                .map(HitsMetadata::hits)
                .orElseGet(Collections::emptyList)
                .stream()
                .map(hit -> Optional.ofNullable(hit.highlight())
                        .orElseGet(Collections::emptyMap))
                .collect(Collectors.toList());
    }
}