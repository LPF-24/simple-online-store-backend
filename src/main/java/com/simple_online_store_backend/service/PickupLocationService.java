package com.simple_online_store_backend.service;

import com.simple_online_store_backend.dto.pickup_location.PickupLocationRequestDTO;
import com.simple_online_store_backend.dto.pickup_location.PickupLocationResponseDTO;
import com.simple_online_store_backend.entity.PickupLocation;
import com.simple_online_store_backend.mapper.PickupLocationMapper;
import com.simple_online_store_backend.repository.PickupLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PickupLocationService {
    private final PickupLocationRepository pickupLocationRepository;
    private final PickupLocationMapper mapper;

    @Transactional
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public PickupLocationResponseDTO addPickupLocation(PickupLocationRequestDTO dto) {
        PickupLocation pickupLocation = mapper.mapRequestTOPickupLocation(dto);
        pickupLocationRepository.save(pickupLocation);
        return mapper.mapPickupLocationToResponseDTO(pickupLocation);
    }
}
