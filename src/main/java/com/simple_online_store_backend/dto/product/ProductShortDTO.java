package com.simple_online_store_backend.dto.product;

import com.simple_online_store_backend.util.SwaggerConstants;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(
        name = "ProductShortDTO",
        description = "Represents a short product view with only essential information (used in lists or order items)."
)
public class ProductShortDTO {

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
            description = SwaggerConstants.PRODUCT_PRICE_DESC,
            example = SwaggerConstants.PRODUCT_PRICE_EXAMPLE,
            minimum = "0.01"
    )
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
