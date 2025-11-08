package com.simple_online_store_backend.dto.product;

import com.simple_online_store_backend.enums.ProductCategory;
import com.simple_online_store_backend.util.SwaggerConstants;
import com.simple_online_store_backend.validation.annotation.ValidProductCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(name = "ProductRequestDTO", description = "Represents data required to create or update a product")
public class ProductRequestDTO {

    @NotEmpty(message = "Product name can't be empty")
    @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
    @Schema(
            description = SwaggerConstants.PRODUCT_REQ_NAME_DESC,
            example = SwaggerConstants.PRODUCT_REQ_NAME_EXAMPLE,
            minLength = 2,
            maxLength = 255
    )
    private String productName;

    @NotEmpty(message = "Product description can't be empty")
    @Size(min = 10, max = 600, message = "Description must be between 10 and 600 characters")
    @Schema(
            description = SwaggerConstants.PRODUCT_REQ_DESC_DESC,
            example = SwaggerConstants.PRODUCT_REQ_DESC_EXAMPLE,
            minLength = 10,
            maxLength = 600
    )
    private String productDescription;

    @NotNull(message = "Price is required")
    @Digits(integer = 8, fraction = 2, message = "The price can include a maximum of 10 digits: 8 before the decimal point and 2 after the decimal point.")
    @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
    @Schema(
            description = SwaggerConstants.PRODUCT_REQ_PRICE_DESC,
            example = SwaggerConstants.PRODUCT_REQ_PRICE_EXAMPLE,
            minimum = "0.01"
    )
    private BigDecimal price;

    @ValidProductCategory
    @NotNull(message = "Product category name can't be empty")
    @Schema(
            description = SwaggerConstants.PRODUCT_REQ_CATEGORY_DESC,
            example = SwaggerConstants.PRODUCT_REQ_CATEGORY_EXAMPLE,
            implementation = ProductCategory.class
    )
    private ProductCategory productCategory;

    @NotNull(message = "Availability can't be empty")
    @Schema(
            description = SwaggerConstants.PRODUCT_REQ_AVAILABILITY_DESC,
            example = SwaggerConstants.PRODUCT_REQ_AVAILABILITY_EXAMPLE
    )
    private Boolean availability;

    public ProductRequestDTO() {
    }

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
