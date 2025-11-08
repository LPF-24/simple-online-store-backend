package com.simple_online_store_backend.dto.product;

import com.simple_online_store_backend.enums.ProductCategory;
import com.simple_online_store_backend.util.SwaggerConstants;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "ProductResponseDTO", description = "Represents product data returned by the API")
public class ProductResponseDTO {

    @Schema(
            description = SwaggerConstants.ID_DESC + "product.",
            example = SwaggerConstants.ID_EXAMPLE,
            minimum = "1"
    )
    private Integer id;

    @Schema(
            description = SwaggerConstants.PRODUCT_NAME_DESC,
            example = SwaggerConstants.PRODUCT_NAME_EXAMPLE
    )
    private String productName;

    @Schema(
            description = SwaggerConstants.PRODUCT_REQ_DESC_DESC,
            example = SwaggerConstants.PRODUCT_REQ_DESC_EXAMPLE
    )
    private String productDescription;

    @Schema(
            description = SwaggerConstants.PRODUCT_PRICE_DESC,
            example = SwaggerConstants.PRODUCT_PRICE_EXAMPLE,
            minimum = "0.01"
    )
    private BigDecimal price;

    @Schema(
            description = SwaggerConstants.PRODUCT_REQ_CATEGORY_DESC,
            example = SwaggerConstants.PRODUCT_REQ_CATEGORY_EXAMPLE,
            implementation = ProductCategory.class
    )
    private ProductCategory productCategory;

    @Schema(
            description = SwaggerConstants.PRODUCT_REQ_AVAILABILITY_DESC,
            example = SwaggerConstants.PRODUCT_REQ_AVAILABILITY_EXAMPLE
    )
    private Boolean availability;

    public ProductResponseDTO() {
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
