package com.simple_online_store_backend.dto.order;

import com.simple_online_store_backend.entity.Address;
import com.simple_online_store_backend.entity.PickupLocation;
import com.simple_online_store_backend.validation.annotation.ValidDeliveryOption;
import com.simple_online_store_backend.validation.annotation.ValidPickupLocation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@ValidDeliveryOption
public class OrderRequestDTO {
    @Schema(description = "Unique identifiers of the products contained in the order", example = "{3, 5, 7}")
    @NotEmpty(message = "You must add at least one product")
    private List<Integer> productsIds;

    @Schema(description = "A pick-up point where the customer can pick up the order.")
    @ValidPickupLocation
    private PickupLocation pickupLocation;

    @Schema(description = "The address to which the order will be delivered (in case of postal or courier delivery)")
    private Address address;

    public OrderRequestDTO() {
    }

    public List<Integer> getProductsIds() {
        return productsIds;
    }

    public void setProductsIds(List<Integer> productsIds) {
        this.productsIds = productsIds;
    }

    public PickupLocation getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(PickupLocation pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }
}
