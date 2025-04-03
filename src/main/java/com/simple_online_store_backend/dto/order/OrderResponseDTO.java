package com.simple_online_store_backend.dto.order;

import com.simple_online_store_backend.dto.person.PersonShortDTO;
import com.simple_online_store_backend.dto.address.AddressResponseDTO;
import com.simple_online_store_backend.dto.pickup_location.PickupLocationResponseDTO;
import com.simple_online_store_backend.dto.product.ProductShortDTO;
import com.simple_online_store_backend.enums.OrderStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class OrderResponseDTO {
    private Integer id;

    private OrderStatus status;

    private PersonShortDTO person;

    private List<ProductShortDTO> products;

    private PickupLocationResponseDTO pickupLocation;

    private AddressResponseDTO address;
}
