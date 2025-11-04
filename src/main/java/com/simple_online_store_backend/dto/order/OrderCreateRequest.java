package com.simple_online_store_backend.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class OrderCreateRequest {

    @NotEmpty(message = "You must add at least one product")
    private List<@NotNull @Min(1) Integer> productIds;

    // одно из двух (валидируй кастомным валидатором или в сервисе)
    private Integer addressId;
    private Integer pickupLocationId;

    public List<Integer> getProductIds() { return productIds; }
    public void setProductIds(List<Integer> productIds) { this.productIds = productIds; }
    public Integer getAddressId() { return addressId; }
    public void setAddressId(Integer addressId) { this.addressId = addressId; }
    public Integer getPickupLocationId() { return pickupLocationId; }
    public void setPickupLocationId(Integer pickupLocationId) { this.pickupLocationId = pickupLocationId; }
}

