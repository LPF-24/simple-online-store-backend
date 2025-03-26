package com.simple_online_store_backend.dto.product;

import com.simple_online_store_backend.enums.ProductCategory;
import com.simple_online_store_backend.validation.annotation.ValidProductCategory;
import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
public class ProductRequestDTO {
    @NotEmpty(message = "Product name can't be empty")
    @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
    private String productName;

    @NotEmpty(message = "Product description can't be empty")
    @Size(min = 10, max = 600, message = "Description must be between 10 and 600 characters")
    private String productDescription;

    @NotNull(message = "Price is required")
    @Digits(integer = 8, fraction = 2, message = "The price can include a maximum of 10 digits: 8 before the decimal point and 2 after the decimal point.")
    //Значение может быть равно 0.01 или больше, но не меньше
    @DecimalMin(value = "0.01", inclusive = true, message = "Price must be at least 0.01")
    private BigDecimal price;

    @ValidProductCategory
    @NotNull(message = "Product category name can't be empty")
    private ProductCategory productCategory;

    @NotNull(message = "Product category name can't be empty")
    private Boolean availability;
}
