package com.simple_online_store_backend.dto.order;

import com.simple_online_store_backend.dto.address.AddressRequestDTO;
import com.simple_online_store_backend.dto.pickup_location.PickupLocationRequestDTO;
import com.simple_online_store_backend.validation.annotation.ValidDeliveryOption;
import com.simple_online_store_backend.validation.annotation.ValidPickupLocation;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@ValidDeliveryOption
public class OrderRequestDTO {
    @NotEmpty(message = "You must add at least one product")
    private List<Integer> productsIds;

    @ValidPickupLocation
    private PickupLocationRequestDTO pickupLocation;

    private AddressRequestDTO address;
}
