package org.example.elasticapi.article.filter;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.json.JsonData;
import io.micrometer.common.util.StringUtils;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;
import org.example.elasticapi.article.document.CarMaster;

@Getter
public enum FilterQueryEnum {
    PRICE_NOT_ZERO("price", "price 가 0 이 아닌 것") {
        @Override
        public void getQuery(BoolQuery.Builder musts, CarMaster.Request request) {
            musts.mustNot(f -> f.term(t -> t.field(getFieldName()).value(0)));
        }
    },
    COUNTRY("area.country", "country") {
        @Override
        public void getQuery(BoolQuery.Builder musts, CarMaster.Request request) {
            if (StringUtils.isEmpty(request.getCountry())) {
                return;
            }
            musts.filter(f -> f.nested(n -> n.path("area").query(q -> q
                    .term(t -> t.field(getFieldName()).value(request.getCountry())))));
        }
    },
    REGION("area.region", "region") {
        @Override
        public void getQuery(BoolQuery.Builder musts, CarMaster.Request request) {
            if (StringUtils.isEmpty(request.getRegion())) {
                return;
            }
            musts.filter(f -> f.nested(n -> n.path("area").query(q -> q
                    .term(t -> t.field(getFieldName()).value(request.getRegion())))));
        }
    },
    STATE("area.state", "state") {
        @Override
        public void getQuery(BoolQuery.Builder musts, CarMaster.Request request) {
            if (StringUtils.isEmpty(request.getState())) {
                return;
            }
            musts.filter(f -> f.nested(n -> n.path("area").query(q -> q
                    .term(t -> t.field(getFieldName()).value(request.getState())))));
        }
    },

    MANUFACTURER("manufacturer", "manufacturer") {
        @Override
        public void getQuery(BoolQuery.Builder musts, CarMaster.Request request) {
            if (StringUtils.isEmpty(request.getManufacturer())) {
                return;
            }
            musts.filter(f -> f.term(t -> t.field(getFieldName()).value(request.getManufacturer())));
        }
    },

    MODEL("model", "model") {
        @Override
        public void getQuery(BoolQuery.Builder musts, CarMaster.Request request) {
            if (StringUtils.isEmpty(request.getModel())) {
                return;
            }
            musts.filter(f -> f.term(t -> t.field(getFieldName()).value(request.getModel())));
        }
    },
    COLOR("color", "color") {
        @Override
        public void getQuery(BoolQuery.Builder musts, CarMaster.Request request) {
            if (StringUtils.isEmpty(request.getColor())) {
                return;
            }
            musts.filter(f -> f.term(t -> t.field(getFieldName()).value(request.getColor())));
        }
    },
    FUEL("fuel", "fuel") {
        @Override
        public void getQuery(BoolQuery.Builder musts, CarMaster.Request request) {
            if (StringUtils.isEmpty(request.getFuel())) {
                return;
            }
            musts.filter(f -> f.term(t -> t.field(getFieldName()).value(request.getFuel())));
        }
    },
    TYPE("type", "type") {
        @Override
        public void getQuery(BoolQuery.Builder musts, CarMaster.Request request) {
            if (StringUtils.isEmpty(request.getType())) {
                return;
            }
            musts.filter(f -> f.term(t -> t.field(getFieldName()).value(request.getType())));
        }
    },
    TRANSMISSION("transmission", "transmission") {
        @Override
        public void getQuery(BoolQuery.Builder musts, CarMaster.Request request) {
            if (StringUtils.isEmpty(request.getTransmission())) {
                return;
            }
            musts.filter(f -> f.term(t -> t.field(getFieldName()).value(request.getTransmission())));
        }
    },
    CYLINDERS("cylinders", "cylinders") {
        @Override
        public void getQuery(BoolQuery.Builder musts, CarMaster.Request request) {
            if (StringUtils.isEmpty(request.getCylinders())) {
                return;
            }
            musts.filter(f -> f.term(t -> t.field(getFieldName()).value(request.getCylinders())));
        }
    },

    YEAR("year", "year") {
        @Override
        public void getQuery(BoolQuery.Builder musts, CarMaster.Request request) {
            if (ObjectUtils.isEmpty(request.getStartYear()) && ObjectUtils.isEmpty(request.getEndYear())) {
                return;
            }
            musts.filter(f -> f
                    .range(r -> r
                            .field(getFieldName())
                            .gte(JsonData.of(request.getStartYear()))
                            .lte(JsonData.of(request.getEndYear()))
                    )
            );
        }
    },
    PRICE("price", "price") {
        @Override
        public void getQuery(BoolQuery.Builder musts, CarMaster.Request request) {
            if (ObjectUtils.isEmpty(request.getStartPrice()) && ObjectUtils.isEmpty(request.getEndPrice())) {
                return;
            }
            musts.filter(f -> f
                    .range(r -> r
                            .field(getFieldName())
                            .gte(JsonData.of(request.getStartPrice()))
                            .lte(JsonData.of(request.getEndPrice()))
                    )
            );
        }
    },

    ODOMETER("odometer", "odometer") {
        @Override
        public void getQuery(BoolQuery.Builder musts, CarMaster.Request request) {
            if (ObjectUtils.isEmpty(request.getStartOdometer()) && ObjectUtils.isEmpty(request.getEndOdometer())) {
                return;
            }
            musts.filter(f -> f
                    .range(r -> r
                            .field(getFieldName())
                            .gte(JsonData.of(request.getStartOdometer()))
                            .lte(JsonData.of(request.getEndOdometer()))
                    )
            );
        }
    };

    private final String fieldName;
    private final String description;

    public abstract void getQuery(BoolQuery.Builder musts, CarMaster.Request request);

    FilterQueryEnum(String fieldName, String description) {
        this.fieldName = fieldName;
        this.description = description;
    }
}
