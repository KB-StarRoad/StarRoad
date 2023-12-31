package com.kb04.starroad.Dto.product;

import com.kb04.starroad.Entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {
    
    private int no;
    private Character type;
    private String attribute;

    private String name;
    private String explain;

    private Integer maxPrice;

    private Double maxRate;
    private Integer maxRatePeriod;
    private Double maxConditionRate;

    private int maxPeriod;
    private Double baseRate;

    private String link;

    public Product toEntity() {
        return Product.builder()
                .no(no)
                .type(type)
                .attribute(attribute)
                .name(name)
                .explain(explain)
                .maxRate(maxRate)
                .maxRatePeriod(maxRatePeriod)
                .maxPeriod(maxPeriod)
                .maxConditionRate(maxConditionRate)
                .link(link)
                .maxPrice(maxPrice)
                .build();
    }

}
