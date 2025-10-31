package com.simple_online_store_backend.dto.product;

import java.math.BigDecimal;

public class ProductShortDTO {
    private Integer id;

    private String productName;

    private BigDecimal price;

    public ProductShortDTO() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
