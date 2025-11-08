package com.simple_online_store_backend.unit.service;

import com.simple_online_store_backend.dto.address.AddressRequestDTO;
import com.simple_online_store_backend.dto.address.AddressResponseDTO;
import com.simple_online_store_backend.entity.Address;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.mapper.AddressMapper;
import com.simple_online_store_backend.repository.AddressRepository;
import com.simple_online_store_backend.repository.PeopleRepository;
import com.simple_online_store_backend.service.AddressService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceTests {

    @Mock AddressRepository addressRepository;
    @Mock PeopleRepository peopleRepository;
    @Mock AddressMapper addressMapper;

    @InjectMocks AddressService addressService;

    private AddressRequestDTO req;
    private Person person;

    @BeforeEach
    void setUp() {
        req = new AddressRequestDTO();
        req.setCity("New York");
        req.setStreet("Main St");
        req.setHouseNumber("12A");
        req.setApartment("34");

        person = new Person();
        person.setId(1);
    }

    // ---------- addAddress

    @Test
    void addAddress_usesExistingAddress_whenFoundByUniqueFields() {
        Address existing = new Address();
        existing.setCity("New York");

        when(peopleRepository.findById(1)).thenReturn(Optional.of(person));
        when(addressRepository.findByCityAndStreetAndHouseNumberAndApartment(
                "New York", "Main St", "12A", "34"
        )).thenReturn(Optional.of(existing));

        AddressResponseDTO resp = new AddressResponseDTO();
        resp.setCity("New York");
        when(addressMapper.mapAddressToResponseDTO(existing)).thenReturn(resp);

        AddressResponseDTO result = addressService.addAddress(req, 1);

        assertEquals("New York", result.getCity());
        assertSame(existing, person.getAddress());
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void addAddress_createsNewAddress_whenNotFound() {
        when(peopleRepository.findById(1)).thenReturn(Optional.of(person));
        when(addressRepository.findByCityAndStreetAndHouseNumberAndApartment(
                "New York", "Main St", "12A", "34"
        )).thenReturn(Optional.empty());

        Address mapped = new Address();
        mapped.setCity("New York");
        when(addressMapper.mapRequestDTOToAddress(req)).thenReturn(mapped);

        AddressResponseDTO resp = new AddressResponseDTO();
        resp.setCity("New York");
        when(addressMapper.mapAddressToResponseDTO(mapped)).thenReturn(resp);

        AddressResponseDTO result = addressService.addAddress(req, 1);

        assertEquals("New York", result.getCity());
        assertSame(mapped, person.getAddress());
        verify(addressRepository).save(mapped);
    }

    @Test
    void addAddress_throwsWhenPersonNotFound() {
        when(peopleRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> addressService.addAddress(req, 99));
    }

    // ---------- updateAddress

    @Test
    void updateAddress_copiesOnlyNonNullFields_andSaves() {
        Address address = new Address();
        address.setCity("OldCity");
        address.setStreet("OldStreet");
        address.setHouseNumber("1");
        address.setApartment("2");

        AddressRequestDTO patch = new AddressRequestDTO();
        patch.setCity("NewCity");

        when(addressRepository.findById(10)).thenReturn(Optional.of(address));

        AddressResponseDTO resp = new AddressResponseDTO();
        resp.setCity("NewCity");
        when(addressMapper.mapAddressToResponseDTO(address)).thenReturn(resp);

        AddressResponseDTO result = addressService.updateAddress(10, patch);

        assertEquals("NewCity", address.getCity());
        assertEquals("OldStreet", address.getStreet());
        assertEquals("1", address.getHouseNumber());
        assertEquals("2", address.getApartment());

        assertEquals("NewCity", result.getCity());
        verify(addressRepository).save(address);
    }

    @Test
    void updateAddress_throwsWhenAddressNotFound() {
        when(addressRepository.findById(123)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> addressService.updateAddress(123, new AddressRequestDTO()));
    }

    @Test
    void deleteAddress_unlinksFromPerson_andDeletesIfUnusedByOthers() {
        Address addr = new Address(); addr.setCity("CityX");
        person.setAddress(addr);

        when(peopleRepository.findById(1)).thenReturn(Optional.of(person));
        when(peopleRepository.existsByAddress(addr)).thenReturn(false);

        addressService.deleteAddress(1);

        assertNull(person.getAddress());
        verify(peopleRepository).save(person);
        verify(addressRepository).delete(addr);
    }

    @Test
    void deleteAddress_unlinksButDoesNotDelete_whenUsedByOthers() {
        Address addr = new Address(); addr.setCity("CityY");
        person.setAddress(addr);

        when(peopleRepository.findById(1)).thenReturn(Optional.of(person));
        when(peopleRepository.existsByAddress(addr)).thenReturn(true);

        addressService.deleteAddress(1);

        assertNull(person.getAddress());
        verify(peopleRepository).save(person);
        verify(addressRepository, never()).delete(any(Address.class));
    }

    @Test
    void deleteAddress_throwsWhenNoAddressSet() {
        when(peopleRepository.findById(1)).thenReturn(Optional.of(person));
        assertThrows(EntityNotFoundException.class, () -> addressService.deleteAddress(1));
        verify(addressRepository, never()).delete(any(Address.class));
    }

    @Test
    void deleteAddress_throwsWhenUserNotFound() {
        when(peopleRepository.findById(42)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> addressService.deleteAddress(42));
    }
}