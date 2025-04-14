package com.simple_online_store_backend.dto.order;

import com.simple_online_store_backend.dto.person.PersonShortDTO;
import com.simple_online_store_backend.dto.address.AddressResponseDTO;
import com.simple_online_store_backend.dto.pickup_location.PickupLocationResponseDTO;
import com.simple_online_store_backend.dto.product.ProductShortDTO;
import com.simple_online_store_backend.enums.OrderStatus;
import com.simple_online_store_backend.util.SwaggerConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class OrderResponseDTO {
    @Schema(description = SwaggerConstants.ID_DESC + " order", example = SwaggerConstants.ID_EXAMPLE)
    private Integer id;

    @Schema(description = "Order status", example = "PENDING")
    private OrderStatus status;

    @Schema(description = "Customer information")
    private PersonShortDTO person;

    @Schema(description = "List of products in the order")
    private List<ProductShortDTO> products;

    @Schema(description = "Pick-up point for ordering")
    private PickupLocationResponseDTO pickupLocation;

    @Schema(description = "Address for order delivery")
    private AddressResponseDTO address;
}
