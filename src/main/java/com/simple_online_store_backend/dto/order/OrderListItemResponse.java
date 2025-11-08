package com.simple_online_store_backend.dto.order;

import com.simple_online_store_backend.enums.OrderStatus;
import com.simple_online_store_backend.util.SwaggerConstants;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "OrderListItemResponse", description = "Represents a short order summary for the orders list view")
public class OrderListItemResponse {

    @Schema(
            description = SwaggerConstants.ID_DESC + "order.",
            example = SwaggerConstants.ID_EXAMPLE,
            minimum = "1"
    )
    private Integer id;

    @Schema(
            description = SwaggerConstants.ORDER_STATUS_DESC,
            implementation = OrderStatus.class,
            example = "PENDING"
    )
    private OrderStatus status;

    @Schema(
            description = SwaggerConstants.ORDER_PRODUCT_COUNT_DESC,
            example = SwaggerConstants.ORDER_PRODUCT_COUNT_EXAMPLE,
            minimum = "0"
    )
    private Integer productCount;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public Integer getProductCount() { return productCount; }
    public void setProductCount(Integer productCount) { this.productCount = productCount; }
}


