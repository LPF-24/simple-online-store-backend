package com.simple_online_store_backend.service;

import com.simple_online_store_backend.dto.PersonRequestDTO;
import com.simple_online_store_backend.dto.PersonResponseDTO;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.mapper.PersonConverter;
import com.simple_online_store_backend.repository.PeopleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PeopleService {
    public final PeopleRepository peopleRepository;
    private final PersonConverter personConverter;

    @Transactional
    public PersonResponseDTO register(PersonRequestDTO personRequestDTO) {
        Person person = personConverter.convertToPerson(personRequestDTO);
        person.setRole("ROLE_USER");
        peopleRepository.saveAndFlush(person);

        return personConverter.convertToResponseDTO(person);
    }
}
