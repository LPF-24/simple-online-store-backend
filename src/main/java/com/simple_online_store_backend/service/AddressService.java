package com.simple_online_store_backend.service;

import com.simple_online_store_backend.dto.address.AddressRequestDTO;
import com.simple_online_store_backend.dto.address.AddressResponseDTO;
import com.simple_online_store_backend.entity.Address;
import com.simple_online_store_backend.mapper.AddressMapper;
import com.simple_online_store_backend.repository.AddressRepository;
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

@Service
@RequiredArgsConstructor
public class AddressService {
    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;
    private final PeopleService peopleService;

    @Transactional
    public AddressResponseDTO addAddress(AddressRequestDTO dto) {
        Address addressToAdd = addressMapper.mapRequestDTOToAddress(dto);
        addressRepository.save(addressToAdd);
        return addressMapper.mapAddressToResponseDTO(addressToAdd);
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
