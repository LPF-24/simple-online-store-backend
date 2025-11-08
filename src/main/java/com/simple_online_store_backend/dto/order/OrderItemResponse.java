package com.simple_online_store_backend.dto.order;

import com.simple_online_store_backend.util.SwaggerConstants;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "OrderItemResponse", description = "Represents a single item within an order")
public class OrderItemResponse {

    @Schema(
            description = SwaggerConstants.PRODUCT_ID_DESC,
            example = SwaggerConstants.ID_EXAMPLE,
            minimum = "1"
    )
    private Integer productId;

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

    @Schema(
            description = SwaggerConstants.PRODUCT_QUANTITY_DESC,
            example = SwaggerConstants.PRODUCT_QUANTITY_EXAMPLE,
            minimum = "1"
    )
    private Integer quantity;

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}


