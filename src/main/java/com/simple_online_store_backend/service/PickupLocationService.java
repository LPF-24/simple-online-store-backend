package com.simple_online_store_backend.service;

import com.simple_online_store_backend.dto.pickup_location.PickupLocationRequestDTO;
import com.simple_online_store_backend.dto.pickup_location.PickupLocationResponseDTO;
import com.simple_online_store_backend.entity.PickupLocation;
import com.simple_online_store_backend.mapper.PickupLocationMapper;
import com.simple_online_store_backend.repository.PickupLocationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PickupLocationService {
    private final PickupLocationRepository pickupLocationRepository;
    private final PickupLocationMapper mapper;

    public List<PickupLocationResponseDTO> getAllPickupLocations(String role) {
        List<PickupLocation> locations;
        if (role.equals("ROLE_ADMIN")) {
            locations = pickupLocationRepository.findAll();
        } else {
            locations = pickupLocationRepository.findByActiveTrue();
        }

        return locations.stream().map(mapper::mapPickupLocationRequestToResponseDTO).toList();
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public PickupLocationResponseDTO addPickupLocation(PickupLocationRequestDTO dto) {
        PickupLocation pickupLocation = mapper.mapRequestTOPickupLocation(dto);
        pickupLocationRepository.save(pickupLocation);
        return mapper.mapPickupLocationRequestToResponseDTO(pickupLocation);
    }

    @Transactional
    @PreAuthorize(("hasRole('ROLE_ADMIN')"))
    public void closePickupLocation(int id) {
        PickupLocation location = pickupLocationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pickup location with this id doesn't exist"));

        location.setActive(false);
    }

    @Transactional
    @PreAuthorize(("hasRole('ROLE_ADMIN')"))
    public void openPickupLocation(int id) {
        PickupLocation location = pickupLocationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pickup location with this id doesn't exist"));

        location.setActive(true);
    }

    @Transactional
    @PreAuthorize(("hasRole('ROLE_ADMIN')"))
    public PickupLocationResponseDTO updatePickupLocation(PickupLocationRequestDTO dto, int id) {
        PickupLocation locationToUpdate = pickupLocationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pickup location with this id doesn't exist"));

        BeanUtils.copyProperties(dto, locationToUpdate, getNullPropertyNames(dto));

        pickupLocationRepository.save(locationToUpdate);
        return mapper.mapPickupLocationRequestToResponseDTO(locationToUpdate);
    }

    private String[] getNullPropertyNames(Object source) {
        try {
            return Arrays.stream(Introspector.getBeanInfo(source.getClass(), Object.class)
                    .getPropertyDescriptors())
                    .map(PropertyDescriptor::getName)
                    .filter(name -> {
                        try {
                            return Objects.isNull(new PropertyDescriptor(name, source.getClass()).getReadMethod().invoke(source));
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .toArray(String[]::new);
        } catch (IntrospectionException e) {
            throw new RuntimeException("Error processing object properties");
        }
    }
}
