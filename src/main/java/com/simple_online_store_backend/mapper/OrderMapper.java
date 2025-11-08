package com.simple_online_store_backend.mapper;

import com.simple_online_store_backend.dto.order.*;
import com.simple_online_store_backend.dto.person.PersonShortDTO;
import com.simple_online_store_backend.dto.product.ProductShortDTO;
import com.simple_online_store_backend.entity.Address;
import com.simple_online_store_backend.entity.Order;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.entity.PickupLocation;
import com.simple_online_store_backend.entity.Product;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class OrderMapper {
    private final ModelMapper modelMapper;
    private final AddressMapper addressMapper;
    private final PickupLocationMapper pickupLocationMapper;

    public OrderMapper(ModelMapper modelMapper,
                       AddressMapper addressMapper,
                       PickupLocationMapper pickupLocationMapper) {
        this.modelMapper = modelMapper;
        this.addressMapper = addressMapper;
        this.pickupLocationMapper = pickupLocationMapper;
    }

    public OrderResponseDTO mapEntityToResponse(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();

        // Базовые поля
        dto.setId(order.getId());
        dto.setStatus(order.getStatus());

        // PickupLocation -> DTO
        PickupLocation pickupLocation = order.getPickupLocation();
        if (pickupLocation != null) {
            dto.setPickupLocation(pickupLocationMapper.mapEntityToResponse(pickupLocation));
        } else {
            dto.setPickupLocation(null); // <-- было dto.setAddress(null) — баг
        }

        // Address -> DTO
        Address address = order.getAddress();
        if (address != null) {
            dto.setAddress(addressMapper.mapAddressToResponseDTO(address));
        } else {
            dto.setAddress(null);
        }

        // Person -> PersonShortDTO
        Person person = order.getPerson();
        if (person != null) {
            PersonShortDTO personShort = new PersonShortDTO();
            personShort.setId(person.getId());
            personShort.setUserName(person.getUserName());
            dto.setPerson(personShort);
        } else {
            dto.setPerson(null);
        }

        // Products -> List<ProductShortDTO>
        List<Product> products = order.getProducts();
        if (products != null) {
            List<ProductShortDTO> productDtos = products.stream()
                    .filter(Objects::nonNull)
                    .map(p -> {
                        ProductShortDTO ps = new ProductShortDTO();
                        ps.setId(p.getId());
                        ps.setProductName(p.getProductName());
                        ps.setPrice(p.getPrice());
                        // при желании можно добавить ещё поля
                        return ps;
                    })
                    .collect(Collectors.toList());
            dto.setProducts(productDtos);
        } else {
            dto.setProducts(List.of()); // или null — но для удобства тестов лучше пустой список
        }

        return dto;
    }

    public OrderListItemResponse toListItem(Order o) {
        OrderListItemResponse dto = new OrderListItemResponse();
        dto.setId(o.getId());
        dto.setStatus(o.getStatus()); // enum
        dto.setProductCount(o.getProducts() != null ? o.getProducts().size() : 0);
        return dto;
    }

    public OrderDetailsResponse toDetails(Order order) {
        OrderDetailsResponse dto = new OrderDetailsResponse();
        dto.setId(order.getId());
        dto.setStatus(order.getStatus()); // enum

        if (order.getPerson() != null) {
            dto.setOwnerId(order.getPerson().getId());
            dto.setOwnerUserName(order.getPerson().getUserName());
        }
        if (order.getAddress() != null) {
            dto.setAddress(addressMapper.mapAddressToResponseDTO(order.getAddress()));
        }
        if (order.getPickupLocation() != null) {
            dto.setPickup(pickupLocationMapper.mapEntityToResponse(order.getPickupLocation()));
        }

        if (order.getProducts() != null) {
            List<OrderItemResponse> items = order.getProducts().stream()
                    .filter(Objects::nonNull)
                    .map(p -> {
                        OrderItemResponse it = new OrderItemResponse();
                        it.setProductId(p.getId());
                        it.setProductName(p.getProductName());
                        it.setPrice(p.getPrice());
                        it.setQuantity(1); // пока так, потом перейдём на OrderItem
                        return it;
                    }).toList();
            dto.setItems(items);
        } else {
            dto.setItems(List.of());
        }
        return dto;
    }
}
