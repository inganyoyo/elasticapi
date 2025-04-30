package org.example.elasticapi.indexer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.TransportUtils;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.RequiredArgsConstructor;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;

@Component
@RequiredArgsConstructor
public class ElasticSearchFactory {
    private final String serverUrl = "http://localhost:9200";
//    private final String apiKey = "dVg1ZWhKWUJ0dmp2TGxraDVRM0w6MXVYMGdkS3NUYkdyaEdEMDFjS1FIdw==";
    private final String apiKey = "RVFlV2lKWUI0SDhfcURiamU2Mm06R2E5Q0MzRmtRSWU3QzBEd0l6b3l3dw==";
    private final String username = "elastic";
    private final String password = "changeme";
//    private final String fingerprint;
    ElasticsearchClient getElasticsearchClient() {
        RestClient restClient = getRestClient();
        return new ElasticsearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper()));
    }

    public RestClient getRestClient() {
//        String fingerprint = "b14e85fff4cc6fecb35d1d58a3cce6d5d7a45d28a867485ab87869686c302961"; // 추가된
//        SSLContext sslContext = TransportUtils.sslContextFromCaFingerprint(fingerprint); // 추가된 부분
        BasicCredentialsProvider credsProv = new BasicCredentialsProvider();
        credsProv.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        return RestClient
                .builder(HttpHost.create(serverUrl))
                .setDefaultHeaders(new Header[]{
                        new BasicHeader("Authorization", "ApiKey " + apiKey)
                })
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
//                        .setSSLContext(sslContext) // 추가된 부분
                        .setDefaultCredentialsProvider(credsProv)
                )
                .build();
    }

}
