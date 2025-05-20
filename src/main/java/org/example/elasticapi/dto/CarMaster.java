package org.example.elasticapi.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
public class CarMaster {
    @Getter
    @Setter
    public static class Request {
        private String indexName;
        private int page;
        private int size;
        private String keyword;

        private String country;
        private String region;
        private String state;
        private String manufacturer;
        private String model;
        private String color;
        private String fuel;
        private String type;
        private String transmission;
        private String cylinders;
        private Integer startYear;
        private Integer endYear;
        private Long startPrice;
        private Long endPrice;
        private Long startOdometer;
        private Long endOdometer;
    }
    @Getter
    @Setter
    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
    public static class Response {
        private String regionUrl;
        private String color;
        private Integer year;
        private String fuel;
        private String type;
        private String manufacturer;
        private String transmission;
        private Long price;
        private String model;
        private String vin;
        private String id;
        private String postingDate;
        private String brand;
        private Long odometer;
        private String imageUrl;
        private String cylinders;
        private String url;
        @JsonProperty("timeStamp")
        private String timeStamp;
        private String condition;
        private String size;
        private String drive;
        private String titleStatus;
        private Area area;
        private List<Double> location;
        @Getter
        @Setter
        public static class Area {
            private String country;
            private String state;
            private String region;
        }
    }

    @Getter
    @Setter
    public static class CompletionRequest {
        private String indexName;
        private String keyword;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
    public static class CompletionResponse {
        private String id;
        private Integer year;
        private String manufacturer;
        private String transmission;
        private String model;
        private String brand;
    }

}
