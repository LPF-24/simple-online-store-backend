package com.simple_online_store_backend.service;

import com.simple_online_store_backend.dto.address.AddressRequestDTO;
import com.simple_online_store_backend.dto.address.AddressResponseDTO;
import com.simple_online_store_backend.entity.Address;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.mapper.AddressMapper;
import com.simple_online_store_backend.repository.AddressRepository;
import com.simple_online_store_backend.repository.PeopleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AddressService {
    private final AddressRepository addressRepository;
    private final PeopleRepository peopleRepository;
    private final AddressMapper addressMapper;

    @Transactional
    public AddressResponseDTO addAddress(AddressRequestDTO dto, int personId) {
        Person person = peopleRepository.findById(personId)
                .orElseThrow(() -> new EntityNotFoundException("Person with this id wasn't found!"));

        Optional<Address> exiting = addressRepository
                .findByCityAndStreetAndHouseNumberAndApartment(dto.getCity(), dto.getStreet(), dto.getHouseNumber(),
                        dto.getApartment());

        if (exiting.isPresent()) {
            person.setAddress(exiting.get());
            return addressMapper.mapAddressToResponseDTO(exiting.get());
        } else {
            Address addressToAdd = addressMapper.mapRequestDTOToAddress(dto);
            addressRepository.save(addressToAdd);
            person.setAddress(addressToAdd);
            return addressMapper.mapAddressToResponseDTO(addressToAdd);
        }
    }

    @Transactional
    public AddressResponseDTO updateAddress(Integer addressId, AddressRequestDTO dto) {
        Address addressToUpdate = addressRepository.findById(addressId).orElseThrow(() ->
                new EntityNotFoundException("There is no address with ID" + addressId));

        BeanUtils.copyProperties(dto, addressToUpdate, getNullPropertyNames(dto));

        addressRepository.save(addressToUpdate);
        return addressMapper.mapAddressToResponseDTO(addressToUpdate);
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
            throw new RuntimeException("error processing object properties", e);
        }
    }
}
