package com.simple_online_store_backend.service;

import com.simple_online_store_backend.dto.pickup_location.PickupLocationRequestDTO;
import com.simple_online_store_backend.dto.pickup_location.PickupLocationResponseDTO;
import com.simple_online_store_backend.entity.PickupLocation;
import com.simple_online_store_backend.exception.ValidationException;
import com.simple_online_store_backend.mapper.PickupLocationMapper;
import com.simple_online_store_backend.repository.PickupLocationRepository;
import jakarta.persistence.EntityNotFoundException;
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
public class PickupLocationService {
    private final PickupLocationRepository pickupLocationRepository;
    private final PickupLocationMapper mapper;

    public PickupLocationService(PickupLocationRepository pickupLocationRepository, PickupLocationMapper mapper) {
        this.pickupLocationRepository = pickupLocationRepository;
        this.mapper = mapper;
    }

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
        boolean exists = pickupLocationRepository.existsByCityIgnoreCaseAndStreetIgnoreCaseAndHouseNumberIgnoreCase(
                dto.getCity(), dto.getStreet(), dto.getHouseNumber()
        );

        if (exists) {
            throw new ValidationException(
                    "Pickup location already exists: " +
                            dto.getCity() + ", " + dto.getStreet() + " " + dto.getHouseNumber()
            );
        }

        PickupLocation pickupLocation = mapper.mapRequestTOPickupLocation(dto);
        if (pickupLocation.getActive() == null) pickupLocation.setActive(true);
        pickupLocationRepository.save(pickupLocation);

        return mapper.mapPickupLocationRequestToResponseDTO(pickupLocation);
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void closePickupLocation(int id) {
        boolean active = pickupLocationRepository.existsByIdAndActiveTrue(id);
        if (!active) {
            boolean exists = pickupLocationRepository.existsById(id);
            if (!exists) {
                throw new EntityNotFoundException("Pick-up location with id " + id + " doesn't exist");
            }
            throw new ValidationException("Pick-up location with id " + id + " is already closed");
        }

        PickupLocation location = pickupLocationRepository.findById(id)
                .orElseThrow();
        location.setActive(false);
        pickupLocationRepository.save(location);
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void openPickupLocation(int id) {
        if (!pickupLocationRepository.existsById(id)) {
            throw new EntityNotFoundException("Pick-up location with id " + id + " doesn't exist");
        }

        if (pickupLocationRepository.existsByIdAndActiveTrue(id)) {
            throw new ValidationException("Pick-up location with id " + id + " is already opened");
        }

        PickupLocation location = pickupLocationRepository.findById(id).orElseThrow();
        location.setActive(true);
        pickupLocationRepository.save(location);
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
