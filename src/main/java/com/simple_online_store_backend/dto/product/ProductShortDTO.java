package com.simple_online_store_backend.dto.product;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ProductShortDTO {
    private Integer id;

    private String productName;

    private BigDecimal price;
}