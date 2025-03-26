package com.simple_online_store_backend.mapper;

import com.simple_online_store_backend.dto.pickup_location.PickupLocationRequestDTO;
import com.simple_online_store_backend.dto.pickup_location.PickupLocationResponseDTO;
import com.simple_online_store_backend.entity.PickupLocation;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PickupLocationMapper {
    private final ModelMapper modelMapper;

    public PickupLocation mapRequestTOPickupLocation(PickupLocationRequestDTO dto) {
        return modelMapper.map(dto, PickupLocation.class);
    }

    public PickupLocationResponseDTO mapPickupLocationToResponseDTO(PickupLocation pickupLocation) {
        return modelMapper.map(pickupLocation, PickupLocationResponseDTO.class);
    }
}
