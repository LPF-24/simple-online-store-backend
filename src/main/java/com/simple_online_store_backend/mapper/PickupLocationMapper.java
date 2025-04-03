package com.simple_online_store_backend.mapper;

import com.simple_online_store_backend.dto.pickup_location.PickupLocationRequestDTO;
import com.simple_online_store_backend.dto.pickup_location.PickupLocationResponseDTO;
import com.simple_online_store_backend.entity.PickupLocation;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PickupLocationMapper {
    private final ModelMapper modelMapper;

    @PostConstruct
    private void init() {
        modelMapper.addMappings(new PropertyMap<PickupLocationRequestDTO, PickupLocation>() {
            @Override
            protected void configure() {
                skip(destination.getId());
                skip(destination.getActive());
                skip(destination.getOrderList());
            }
        });
    }

    public PickupLocation mapRequestTOPickupLocation(PickupLocationRequestDTO dto) {
        return modelMapper.map(dto, PickupLocation.class);
    }

    public PickupLocationResponseDTO mapEntityToResponse(PickupLocation pickupLocation) {
        return modelMapper.map(pickupLocation, PickupLocationResponseDTO.class);
    }

    public PickupLocationResponseDTO mapPickupLocationRequestToResponseDTO(PickupLocation pickupLocation) {
        return modelMapper.map(pickupLocation, PickupLocationResponseDTO.class);
    }
}
