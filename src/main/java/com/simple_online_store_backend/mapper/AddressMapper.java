package com.simple_online_store_backend.mapper;

import com.simple_online_store_backend.dto.address.AddressRequestDTO;
import com.simple_online_store_backend.dto.address.AddressResponseDTO;
import com.simple_online_store_backend.entity.Address;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AddressMapper {
    private final ModelMapper modelMapper;

    public Address mapRequestDTOToAddress(AddressRequestDTO dto) {
        return modelMapper.map(dto, Address.class);
    }

    public AddressResponseDTO mapAddressToResponseDTO(Address address) {
        return modelMapper.map(address, AddressResponseDTO.class);
    }
}
