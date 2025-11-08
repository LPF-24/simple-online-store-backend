package com.simple_online_store_backend.unit.service;

import com.simple_online_store_backend.dto.pickup_location.PickupLocationRequestDTO;
import com.simple_online_store_backend.dto.pickup_location.PickupLocationResponseDTO;
import com.simple_online_store_backend.entity.PickupLocation;
import com.simple_online_store_backend.exception.ValidationException;
import com.simple_online_store_backend.mapper.PickupLocationMapper;
import com.simple_online_store_backend.repository.PickupLocationRepository;
import com.simple_online_store_backend.service.PickupLocationService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PickupLocationServiceTests {

    @Mock PickupLocationRepository pickupLocationRepository;
    @Mock PickupLocationMapper mapper;

    @InjectMocks PickupLocationService pickupLocationService;

    // ---------- getAllPickupLocations

    @Test
    void getAllPickupLocations_adminGetsAll() {
        PickupLocation a = active(true);
        PickupLocation b = active(false);
        when(pickupLocationRepository.findAll()).thenReturn(List.of(a, b));

        PickupLocationResponseDTO ra = new PickupLocationResponseDTO();
        PickupLocationResponseDTO rb = new PickupLocationResponseDTO();
        when(mapper.mapPickupLocationRequestToResponseDTO(a)).thenReturn(ra);
        when(mapper.mapPickupLocationRequestToResponseDTO(b)).thenReturn(rb);

        var out = pickupLocationService.getAllPickupLocations("ROLE_ADMIN");

        assertEquals(2, out.size());
        verify(pickupLocationRepository).findAll();
        verify(mapper).mapPickupLocationRequestToResponseDTO(a);
        verify(mapper).mapPickupLocationRequestToResponseDTO(b);
    }

    @Test
    void getAllPickupLocations_userGetsOnlyActive() {
        PickupLocation a = active(true);
        when(pickupLocationRepository.findByActiveTrue()).thenReturn(List.of(a));

        PickupLocationResponseDTO ra = new PickupLocationResponseDTO();
        when(mapper.mapPickupLocationRequestToResponseDTO(a)).thenReturn(ra);

        var out = pickupLocationService.getAllPickupLocations("ROLE_USER");

        assertEquals(1, out.size());
        verify(pickupLocationRepository).findByActiveTrue();
    }

    // ---------- addPickupLocation

    @Test
    void addPickupLocation_throwsWhenExists() {
        var dto = req("City", "Street", "1");
        when(pickupLocationRepository.existsByCityIgnoreCaseAndStreetIgnoreCaseAndHouseNumberIgnoreCase(
                "City", "Street", "1"
        )).thenReturn(true);

        assertThrows(ValidationException.class, () -> pickupLocationService.addPickupLocation(dto));
        verify(pickupLocationRepository, never()).save(any());
    }

    @Test
    void addPickupLocation_createsNew_andDefaultsActiveTrue_whenNull() {
        var dto = req("City", "Street", "1");

        when(pickupLocationRepository.existsByCityIgnoreCaseAndStreetIgnoreCaseAndHouseNumberIgnoreCase(
                "City", "Street", "1"
        )).thenReturn(false);

        PickupLocation mapped = new PickupLocation();
        mapped.setActive(null); // сервис должен поставить true
        when(mapper.mapRequestTOPickupLocation(dto)).thenReturn(mapped);

        PickupLocationResponseDTO resp = new PickupLocationResponseDTO();
        when(mapper.mapPickupLocationRequestToResponseDTO(mapped)).thenReturn(resp);

        var out = pickupLocationService.addPickupLocation(dto);

        assertSame(resp, out);
        assertTrue(mapped.getActive());
        verify(pickupLocationRepository).save(mapped);
    }

    // ---------- closePickupLocation

    @Test
    void closePickupLocation_ok_whenActive() {
        PickupLocation loc = active(true);
        when(pickupLocationRepository.existsByIdAndActiveTrue(5)).thenReturn(true);
        when(pickupLocationRepository.findById(5)).thenReturn(Optional.of(loc));

        pickupLocationService.closePickupLocation(5);

        assertFalse(loc.getActive());
        verify(pickupLocationRepository).save(loc);
    }

    @Test
    void closePickupLocation_throwsNotFound_whenNoSuchId() {
        when(pickupLocationRepository.existsByIdAndActiveTrue(5)).thenReturn(false);
        when(pickupLocationRepository.existsById(5)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> pickupLocationService.closePickupLocation(5));
    }

    @Test
    void closePickupLocation_throwsValidation_whenAlreadyClosed() {
        when(pickupLocationRepository.existsByIdAndActiveTrue(5)).thenReturn(false);
        when(pickupLocationRepository.existsById(5)).thenReturn(true);

        assertThrows(ValidationException.class, () -> pickupLocationService.closePickupLocation(5));
    }

    // ---------- openPickupLocation

    @Test
    void openPickupLocation_ok_whenExistsAndClosed() {
        PickupLocation loc = active(false);
        when(pickupLocationRepository.existsById(7)).thenReturn(true);
        when(pickupLocationRepository.existsByIdAndActiveTrue(7)).thenReturn(false);
        when(pickupLocationRepository.findById(7)).thenReturn(Optional.of(loc));

        pickupLocationService.openPickupLocation(7);

        assertTrue(loc.getActive());
        verify(pickupLocationRepository).save(loc);
    }

    @Test
    void openPickupLocation_throwsNotFound_whenNoSuchId() {
        when(pickupLocationRepository.existsById(7)).thenReturn(false);
        assertThrows(EntityNotFoundException.class, () -> pickupLocationService.openPickupLocation(7));
    }

    @Test
    void openPickupLocation_throwsValidation_whenAlreadyOpen() {
        when(pickupLocationRepository.existsById(7)).thenReturn(true);
        when(pickupLocationRepository.existsByIdAndActiveTrue(7)).thenReturn(true);
        assertThrows(ValidationException.class, () -> pickupLocationService.openPickupLocation(7));
    }

    // ---------- updatePickupLocation

    @Test
    void updatePickupLocation_copiesOnlyNonNull_andSaves() {
        PickupLocation existing = new PickupLocation();
        existing.setCity("Old");
        existing.setStreet("OldSt");
        existing.setHouseNumber("1");
        existing.setActive(false);

        PickupLocationRequestDTO patch = new PickupLocationRequestDTO();
        patch.setCity("New");
        patch.setStreet(null);
        patch.setHouseNumber(null);

        when(pickupLocationRepository.findById(10)).thenReturn(Optional.of(existing));

        PickupLocationResponseDTO resp = new PickupLocationResponseDTO();
        when(mapper.mapPickupLocationRequestToResponseDTO(existing)).thenReturn(resp);

        var out = pickupLocationService.updatePickupLocation(patch, 10);

        assertSame(resp, out);
        assertEquals("New", existing.getCity());
        assertEquals("OldSt", existing.getStreet());
        assertEquals("1", existing.getHouseNumber());
        verify(pickupLocationRepository).save(existing);
    }

    @Test
    void updatePickupLocation_throwsWhenNotFound() {
        when(pickupLocationRepository.findById(10)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> pickupLocationService.updatePickupLocation(new PickupLocationRequestDTO(), 10));
    }

    // ---------- helpers

    private PickupLocation active(boolean flag) {
        PickupLocation p = new PickupLocation();
        p.setActive(flag);
        return p;
    }

    private PickupLocationRequestDTO req(String city, String street, String house) {
        PickupLocationRequestDTO dto = new PickupLocationRequestDTO();
        dto.setCity(city);
        dto.setStreet(street);
        dto.setHouseNumber(house);
        return dto;
    }
}

