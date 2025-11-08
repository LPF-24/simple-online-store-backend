package com.simple_online_store_backend.dto.order;

import com.simple_online_store_backend.dto.address.AddressResponseDTO;
import com.simple_online_store_backend.dto.pickup_location.PickupLocationResponseDTO;
import com.simple_online_store_backend.enums.OrderStatus;
import com.simple_online_store_backend.util.SwaggerConstants;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "OrderDetailsResponse", description = "Подробная информация о заказе")
public class OrderDetailsResponse {

    @Schema(
            description = SwaggerConstants.ID_DESC + "order",
            example = SwaggerConstants.ID_EXAMPLE,
            minimum = "1"
    )
    private Integer id;

    @Schema(
            description = SwaggerConstants.ORDER_STATUS_DESC,
            implementation = OrderStatus.class,
            example = "PENDING"
    )
    private OrderStatus status;

    @Schema(
            description = SwaggerConstants.ORDER_OWNER_ID_DESC,
            example = SwaggerConstants.ID_EXAMPLE,
            minimum = "1"
    )
    private Integer ownerId;

    @Schema(
            description = SwaggerConstants.ORDER_OWNER_USERNAME_DESC,
            example = SwaggerConstants.USERNAME_EXAMPLE
    )
    private String ownerUserName;

    @Schema(
            description = SwaggerConstants.ORDER_ADDRESS_DESC,
            nullable = true
    )
    private AddressResponseDTO address;

    @Schema(
            description = SwaggerConstants.ORDER_PICKUP_DESC,
            nullable = true
    )
    private PickupLocationResponseDTO pickup;

    @ArraySchema(
            arraySchema = @Schema(description = SwaggerConstants.ORDER_ITEMS_DESC),
            schema = @Schema(implementation = OrderItemResponse.class),
            minItems = 1
    )
    private List<OrderItemResponse> items;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public Integer getOwnerId() { return ownerId; }
    public void setOwnerId(Integer ownerId) { this.ownerId = ownerId; }
    public String getOwnerUserName() { return ownerUserName; }
    public void setOwnerUserName(String ownerUserName) { this.ownerUserName = ownerUserName; }
    public AddressResponseDTO getAddress() { return address; }
    public void setAddress(AddressResponseDTO address) { this.address = address; }
    public PickupLocationResponseDTO getPickup() { return pickup; }
    public void setPickup(PickupLocationResponseDTO pickup) { this.pickup = pickup; }
    public List<OrderItemResponse> getItems() { return items; }
    public void setItems(List<OrderItemResponse> items) { this.items = items; }
}

