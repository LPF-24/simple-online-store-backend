package com.simple_online_store_backend.dto.order;

import com.simple_online_store_backend.dto.address.AddressResponseDTO;
import com.simple_online_store_backend.dto.pickup_location.PickupLocationResponseDTO;
import com.simple_online_store_backend.enums.OrderStatus;

import java.util.List;

public class OrderDetailsResponse {
    private Integer id;
    private OrderStatus status;

    private Integer ownerId;
    private String  ownerUserName;

    private AddressResponseDTO address;
    private PickupLocationResponseDTO pickup;
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

