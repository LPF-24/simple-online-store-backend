package com.simple_online_store_backend.dto.product;

import com.simple_online_store_backend.enums.ProductCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
public class ProductResponseDTO {
    private String productName;

    private String productDescription;

    private BigDecimal price;

    private ProductCategory productCategory;

    private Boolean availability;
}
