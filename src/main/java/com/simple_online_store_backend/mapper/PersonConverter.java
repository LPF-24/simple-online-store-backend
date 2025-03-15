package com.simple_online_store_backend.mapper;

import com.simple_online_store_backend.dto.PersonRequestDTO;
import com.simple_online_store_backend.dto.PersonResponseDTO;
import com.simple_online_store_backend.entity.Person;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PersonConverter {
    private final ModelMapper modelMapper;

    public Person convertToPerson(PersonRequestDTO dto) {
        return modelMapper.map(dto, Person.class);
    }

    public PersonResponseDTO convertToResponseDTO(Person person) {
        return modelMapper.map(person, PersonResponseDTO.class);
    }
}
