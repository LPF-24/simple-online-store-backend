package com.simple_online_store_backend.mapper;

import com.simple_online_store_backend.dto.person.PersonRequestDTO;
import com.simple_online_store_backend.dto.person.PersonResponseDTO;
import com.simple_online_store_backend.dto.pickup_location.PickupLocationRequestDTO;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.entity.PickupLocation;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PersonConverter {
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    private void init() {
        modelMapper.addMappings(new PropertyMap<PersonRequestDTO, Person>() {
            @Override
            protected void configure() {
                skip(destination.getId());
                skip(destination.getRole());
                skip(destination.getIsDeleted());
                skip(destination.getOrders());
                skip(destination.getAddress());
            }
        });
    }

    public Person convertToPerson(PersonRequestDTO dto) {
        return modelMapper.map(dto, Person.class);
    }

    public Person convertToPersonToRequest(PersonRequestDTO dto) {
        Person person = modelMapper.map(dto, Person.class);
        person.setPassword(passwordEncoder.encode(dto.getPassword()));
        return person;
    }

    public PersonResponseDTO convertToResponseDTO(Person person) {
        return modelMapper.map(person, PersonResponseDTO.class);
    }
}
