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
    public void register(PersonRequestDTO personRequestDTO) {
        System.out.println("Method register started");
        System.out.println("DTO: " + personRequestDTO);
        Person person = personConverter.convertToPersonToRequest(personRequestDTO);
        person.setRole("ROLE_USER");
        System.out.println("Middle of the method register");
        peopleRepository.saveAndFlush(person);
        System.out.println("Method register ended");
    }


}
