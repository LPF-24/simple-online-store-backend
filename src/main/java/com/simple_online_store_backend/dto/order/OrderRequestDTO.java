package com.simple_online_store_backend.dto.order;

import com.simple_online_store_backend.entity.Address;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.entity.PickupLocation;
import com.simple_online_store_backend.enums.OrderStatus;
import com.simple_online_store_backend.validation.annotation.AccountActive;
import com.simple_online_store_backend.validation.annotation.ValidDeliveryOption;
import com.simple_online_store_backend.validation.annotation.ValidPickupLocation;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@ValidDeliveryOption
public class OrderRequestDTO {
    /*@NotNull(message = "Order status is required")
    private OrderStatus status;*/

    @NotNull(message = "An order cannot be placed without a customer")
    @AccountActive
    private Person person;

    @ValidPickupLocation
    private PickupLocation pickupLocation;

    private Address address;
}
