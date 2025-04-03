package com.simple_online_store_backend.mapper;

import com.simple_online_store_backend.dto.person.PersonShortDTO;
import com.simple_online_store_backend.dto.order.OrderRequestDTO;
import com.simple_online_store_backend.dto.order.OrderResponseDTO;
import com.simple_online_store_backend.dto.product.ProductShortDTO;
import com.simple_online_store_backend.entity.Address;
import com.simple_online_store_backend.entity.Order;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.entity.PickupLocation;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

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

        // Копируем простые поля
        dto.setStatus(order.getStatus());

        // Маппим pickupLocation → PickupLocationResponseDTO
        PickupLocation pickupLocation = order.getPickupLocation();
        if (pickupLocation != null) {
            dto.setPickupLocation(pickupLocationMapper.mapEntityToResponse(pickupLocation));
        } else {
            dto.setPickupLocation(null);
        }

        // Маппим address → AddressResponseDTO
        Address address = order.getAddress();
        if (address != null) {
            dto.setAddress(addressMapper.mapAddressToResponseDTO(address));
        } else {
            dto.setAddress(null);
        }

        // Маппим person → PersonShortDTO
        Person person = order.getPerson();
        if (person != null) {
            PersonShortDTO personShort = new PersonShortDTO();
            personShort.setId(person.getId());
            personShort.setUserName(person.getUserName());
            dto.setPerson(personShort);
        }

        // ✅ Маппим products → ProductShortDTO
        List<ProductShortDTO> productDTOs = order.getProducts().stream()
                .map(product -> {
                    ProductShortDTO shortDTO = new ProductShortDTO();
                    shortDTO.setId(product.getId());
                    shortDTO.setProductName(product.getProductName());
                    shortDTO.setPrice(product.getPrice());
                    return shortDTO;
                }).toList();

        dto.setProducts(productDTOs);

        return dto;
    }
}
