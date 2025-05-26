package org.example.elasticapi.article.filter;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import lombok.Getter;
import org.example.elasticapi.common.dto.SearchRequestDTO;

@Getter
public enum ArticleFilterQueryEnum {
    PRICE_NOT_ZERO("price", "price 가 0 이 아닌 것") {
        @Override
        public void applyFilter(BoolQuery.Builder musts, SearchRequestDTO request) {
            musts.mustNot(f -> f.term(t -> t.field(getFieldName()).value(0)));
        }
    },
    PRICE_NOT_ZERO2("price2", "price2 가 0 이 아닌 것") {
        @Override
        public void applyFilter(BoolQuery.Builder musts, SearchRequestDTO request) {
            musts.mustNot(f -> f.term(t -> t.field(getFieldName()).value(0)));
        }
    };

//    MANUFACTURER("title", "title") {
//        @Override
//        public void applyFilter(BoolQuery.Builder musts, SearchRequestDTO request) {
//            if (StringUtils.isEmpty(request.getManufacturer())) {
//                return;
//            }
//            musts.filter(f -> f.term(t -> t.field(getFieldName()).value(request.getManufacturer())));
//        }
//    },
//
//    MODEL("model", "model") {
//        @Override
//        public void applyFilter(BoolQuery.Builder musts, CarMaster.Request request) {
//            if (StringUtils.isEmpty(request.getModel())) {
//                return;
//            }
//            musts.filter(f -> f.term(t -> t.field(getFieldName()).value(request.getModel())));
//        }
//    },
//    COLOR("color", "color") {
//        @Override
//        public void applyFilter(BoolQuery.Builder musts, CarMaster.Request request) {
//            if (StringUtils.isEmpty(request.getColor())) {
//                return;
//            }
//            musts.filter(f -> f.term(t -> t.field(getFieldName()).value(request.getColor())));
//        }
//    },
//    FUEL("fuel", "fuel") {
//        @Override
//        public void applyFilter(BoolQuery.Builder musts, CarMaster.Request request) {
//            if (StringUtils.isEmpty(request.getFuel())) {
//                return;
//            }
//            musts.filter(f -> f.term(t -> t.field(getFieldName()).value(request.getFuel())));
//        }
//    },
//    TYPE("type", "type") {
//        @Override
//        public void applyFilter(BoolQuery.Builder musts, CarMaster.Request request) {
//            if (StringUtils.isEmpty(request.getType())) {
//                return;
//            }
//            musts.filter(f -> f.term(t -> t.field(getFieldName()).value(request.getType())));
//        }
//    },
//    TRANSMISSION("transmission", "transmission") {
//        @Override
//        public void applyFilter(BoolQuery.Builder musts, CarMaster.Request request) {
//            if (StringUtils.isEmpty(request.getTransmission())) {
//                return;
//            }
//            musts.filter(f -> f.term(t -> t.field(getFieldName()).value(request.getTransmission())));
//        }
//    },
//    CYLINDERS("cylinders", "cylinders") {
//        @Override
//        public void applyFilter(BoolQuery.Builder musts, CarMaster.Request request) {
//            if (StringUtils.isEmpty(request.getCylinders())) {
//                return;
//            }
//            musts.filter(f -> f.term(t -> t.field(getFieldName()).value(request.getCylinders())));
//        }
//    };

//    YEAR("year", "year") {
//        @Override
//        public void applyFilter(BoolQuery.Builder musts, CarMaster.Request request) {
//            if (ObjectUtils.isEmpty(request.getStartYear()) && ObjectUtils.isEmpty(request.getEndYear())) {
//                return;
//            }
//            musts.filter(f -> f
//                    .range(r -> r
//                            .field(getFieldName())
//                            .gte(JsonData.of(request.getStartYear()))
//                            .lte(JsonData.of(request.getEndYear()))
//                    )
//            );
//        }
//    },
//    PRICE("price", "price") {
//        @Override
//        public void applyFilter(BoolQuery.Builder musts, CarMaster.Request request) {
//            if (ObjectUtils.isEmpty(request.getStartPrice()) && ObjectUtils.isEmpty(request.getEndPrice())) {
//                return;
//            }
//            musts.filter(f -> f
//                    .range(r -> r
//                            .field(getFieldName())
//                            .gte(JsonData.of(request.getStartPrice()))
//                            .lte(JsonData.of(request.getEndPrice()))
//                    )
//            );
//        }
//    };

//    ODOMETER("odometer", "odometer") {
//        @Override
//        public void applyFilter(BoolQuery.Builder musts, CarMaster.Request request) {
//            if (ObjectUtils.isEmpty(request.getStartOdometer()) && ObjectUtils.isEmpty(request.getEndOdometer())) {
//                return;
//            }
//            musts.filter(f -> f
//                    .range(r -> r
//                            .field(getFieldName())
//                            .gte(JsonData.of(request.getStartOdometer()))
//                            .lte(JsonData.of(request.getEndOdometer()))
//                    )
//            );
//        }
//    };

    private final String fieldName;
    private final String description;

    ArticleFilterQueryEnum(String fieldName, String description) {
        this.fieldName = fieldName;
        this.description = description;
    }

    public abstract void applyFilter(BoolQuery.Builder musts, SearchRequestDTO request);
}
