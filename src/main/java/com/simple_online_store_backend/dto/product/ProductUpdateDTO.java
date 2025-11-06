package com.simple_online_store_backend.dto.product;

import com.simple_online_store_backend.enums.ProductCategory;
import com.simple_online_store_backend.validation.annotation.ValidProductCategory;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ProductUpdateDTO {

    @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
    private String productName;

    @Size(min = 10, max = 600, message = "Description must be between 10 and 600 characters")
    private String productDescription;

    @Digits(integer = 8, fraction = 2, message = "The price can include a maximum of 10 digits: 8 before the decimal point and 2 after the decimal point.")
    @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
    private BigDecimal price;

    @ValidProductCategory
    private ProductCategory productCategory;

    private Boolean availability;

    public ProductUpdateDTO() {}

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public ProductCategory getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(ProductCategory productCategory) {
        this.productCategory = productCategory;
    }

    public Boolean getAvailability() {
        return availability;
    }

    public void setAvailability(Boolean availability) {
        this.availability = availability;
    }
}
