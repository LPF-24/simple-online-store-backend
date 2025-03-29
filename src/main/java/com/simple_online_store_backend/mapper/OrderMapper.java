package com.simple_online_store_backend.mapper;

import com.simple_online_store_backend.dto.PersonShortDTO;
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

        // 1. Копируем простые поля
        dto.setStatus(order.getStatus());
        //dto.setPickupLocation(order.getPickupLocation());
        PickupLocation pickupLocation = order.getPickupLocation();
        if (pickupLocation != null) {
            dto.setPickupLocation(pickupLocationMapper.mapEntityToResponse(pickupLocation));
        } else {
            // либо ничего не делаем
            dto.setAddress(null);
        }
        // Если у тебя есть отдельный mapper для Address -> AddressResponseDTO, вызывай его:
        Address address = order.getAddress();
        if (address != null) {
            dto.setAddress(addressMapper.mapAddressToResponseDTO(address));
        } else {
            // либо ничего не делаем
            dto.setAddress(null);
        }
        // Либо, если пока что нет, можешь просто передавать сразу order.getAddress()
        // dto.setAddress(order.getAddress());

        // 2. Собираем PersonShortDTO вручную
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
