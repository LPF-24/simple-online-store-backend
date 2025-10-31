package com.simple_online_store_backend.mapper;

import com.simple_online_store_backend.dto.pickup_location.PickupLocationRequestDTO;
import com.simple_online_store_backend.dto.pickup_location.PickupLocationResponseDTO;
import com.simple_online_store_backend.entity.PickupLocation;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class PickupLocationMapper {
    private final ModelMapper modelMapper;

    public PickupLocationMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
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
