package com.simple_online_store_backend.dto.order;

import com.simple_online_store_backend.entity.Address;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.entity.PickupLocation;
import com.simple_online_store_backend.entity.Product;
import com.simple_online_store_backend.enums.OrderStatus;
import com.simple_online_store_backend.validation.annotation.AccountActive;
import com.simple_online_store_backend.validation.annotation.ValidDeliveryOption;
import com.simple_online_store_backend.validation.annotation.ValidPickupLocation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
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
}
