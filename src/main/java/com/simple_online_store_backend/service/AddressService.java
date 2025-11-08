package com.simple_online_store_backend.service;

import com.simple_online_store_backend.dto.address.AddressRequestDTO;
import com.simple_online_store_backend.dto.address.AddressResponseDTO;
import com.simple_online_store_backend.entity.Address;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.mapper.AddressMapper;
import com.simple_online_store_backend.repository.AddressRepository;
import com.simple_online_store_backend.repository.PeopleRepository;
import jakarta.persistence.EntityNotFoundException;
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
public class AddressService {
    private final AddressRepository addressRepository;
    private final PeopleRepository peopleRepository;
    private final AddressMapper addressMapper;

    public AddressService(AddressRepository addressRepository, PeopleRepository peopleRepository, AddressMapper addressMapper) {
        this.addressRepository = addressRepository;
        this.peopleRepository = peopleRepository;
        this.addressMapper = addressMapper;
    }

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

    @Transactional
    public void deleteAddress(int userId) {
        Person person = peopleRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Address address = person.getAddress();
        if (address == null) {
            throw new EntityNotFoundException("User has not yet specified any address");
        }

        person.setAddress(null);
        peopleRepository.save(person);

        boolean usedByOthers = peopleRepository.existsByAddress(address);
        if (!usedByOthers) {
            addressRepository.delete(address);
        }
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
