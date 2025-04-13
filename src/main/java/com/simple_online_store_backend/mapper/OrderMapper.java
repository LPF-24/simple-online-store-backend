package com.simple_online_store_backend.mapper;

import com.simple_online_store_backend.dto.person.PersonShortDTO;
import com.simple_online_store_backend.dto.order.OrderRequestDTO;
import com.simple_online_store_backend.dto.order.OrderResponseDTO;
import com.simple_online_store_backend.entity.Address;
import com.simple_online_store_backend.entity.Order;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.entity.PickupLocation;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderMapper {
    private final ModelMapper modelMapper;
    private final AddressMapper addressMapper;
    private final PickupLocationMapper pickupLocationMapper;

    public Order mapRequestToOrder(OrderRequestDTO dto) {
        return modelMapper.map(dto, Order.class);
    }

    public OrderResponseDTO mapEntityToResponse(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();

        // Map basic scalar fields from entity to DTO
        dto.setId(order.getId());
        dto.setStatus(order.getStatus());
        // Use mapper for PickupLocation -> PickupLocationResponseDTO
        PickupLocation pickupLocation = order.getPickupLocation();
        // Check for null to avoid NullPointerException during mapping
        if (pickupLocation != null) {
            dto.setPickupLocation(pickupLocationMapper.mapEntityToResponse(pickupLocation));
        } else {
            dto.setAddress(null);
        }
        Address address = order.getAddress();
        if (address != null) {
            dto.setAddress(addressMapper.mapAddressToResponseDTO(address));
        } else {
            dto.setAddress(null);
        }

        // Manually map person entity to PersonShortDTO
        Person person = order.getPerson();
        if (person != null) {
            PersonShortDTO personShort = new PersonShortDTO();
            personShort.setId(person.getId());
            personShort.setUserName(person.getUserName());
            dto.setPerson(personShort);
        }

        return dto;
    }
}
