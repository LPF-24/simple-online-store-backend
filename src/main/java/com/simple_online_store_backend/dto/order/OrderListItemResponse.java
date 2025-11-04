package com.simple_online_store_backend.dto.order;

import com.simple_online_store_backend.enums.OrderStatus;

public class OrderListItemResponse {
    private Integer id;

    private OrderStatus status;

    private Integer productCount;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public Integer getProductCount() { return productCount; }
    public void setProductCount(Integer productCount) { this.productCount = productCount; }
}


