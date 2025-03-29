package com.simple_online_store_backend.dto.order;

import com.simple_online_store_backend.dto.PersonResponseDTO;
import com.simple_online_store_backend.dto.address.AddressResponseDTO;
import com.simple_online_store_backend.entity.PickupLocation;
import com.simple_online_store_backend.enums.OrderStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class OrderResponseDTO {
    private OrderStatus status;

    private PersonResponseDTO person;

    private PickupLocation pickupLocation;

    private AddressResponseDTO address;
}
