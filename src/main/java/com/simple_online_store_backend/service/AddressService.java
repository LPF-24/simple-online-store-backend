package com.simple_online_store_backend.service;

import com.simple_online_store_backend.dto.address.AddressRequestDTO;
import com.simple_online_store_backend.dto.address.AddressResponseDTO;
import com.simple_online_store_backend.entity.Address;
import com.simple_online_store_backend.mapper.AddressMapper;
import com.simple_online_store_backend.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddressService {
    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;

    @Transactional
    public AddressResponseDTO addAddress(AddressRequestDTO dto) {
        Address addressToAdd = addressMapper.mapRequestDTOToAddress(dto);
        addressRepository.save(addressToAdd);
        return addressMapper.mapAddressToResponseDTO(addressToAdd);
    }
}
