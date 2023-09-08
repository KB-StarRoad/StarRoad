package com.kb04.starroad.Entity;

import lombok.*;

import javax.persistence.*;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SequenceGenerator(name = "product_seq", sequenceName = "product_seq")
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
    @Column(nullable = false)
    private int no;

    @Column(columnDefinition = "char(1)", nullable = false)
    private Character type;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 1000, nullable = false)
    private String explain;

    @Column(length = 50, nullable = false)
    private String attribute;

    @Column(name = "min_period", nullable = false)
    private int minPeriod;

    @Column(name = "max_period", nullable = false)
    private int maxPeriod;

    @Column(name = "min_price", nullable = false)
    private int minPrice;

    @Column(name = "max_price")
    private int maxPrice;

    @Column(length = 5000, nullable = false)
    private String link;

    @Column(name = "max_rate", nullable = false)
    private Double maxRate;

    @Column(name = "max_rate_period")
    private int maxRatePeriod;


}