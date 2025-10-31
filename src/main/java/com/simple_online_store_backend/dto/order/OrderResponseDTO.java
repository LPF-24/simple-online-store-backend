package com.simple_online_store_backend.dto.order;

import com.simple_online_store_backend.dto.person.PersonShortDTO;
import com.simple_online_store_backend.dto.address.AddressResponseDTO;
import com.simple_online_store_backend.dto.pickup_location.PickupLocationResponseDTO;
import com.simple_online_store_backend.dto.product.ProductShortDTO;
import com.simple_online_store_backend.enums.OrderStatus;
import com.simple_online_store_backend.util.SwaggerConstants;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

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

    public OrderResponseDTO() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public PersonShortDTO getPerson() {
        return person;
    }

    public void setPerson(PersonShortDTO person) {
        this.person = person;
    }

    public List<ProductShortDTO> getProducts() {
        return products;
    }

    public void setProducts(List<ProductShortDTO> products) {
        this.products = products;
    }

    public PickupLocationResponseDTO getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(PickupLocationResponseDTO pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public AddressResponseDTO getAddress() {
        return address;
    }

    public void setAddress(AddressResponseDTO address) {
        this.address = address;
    }
}
