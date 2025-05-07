package org.example.elasticapi.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;
public class CarMaster {
    @Getter
    @Setter
    public static class Request {
        private String indexName;
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
}