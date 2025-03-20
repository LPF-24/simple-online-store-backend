package com.simple_online_store_backend.service;

import com.simple_online_store_backend.dto.PersonRequestDTO;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.mapper.PersonConverter;
import com.simple_online_store_backend.repository.PeopleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PeopleService {
    private final PeopleRepository peopleRepository;
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

    @Transactional
    public void deactivateUserAccount(int userId) {
        Person person = peopleRepository.findById(userId).orElseThrow(
                () -> new UsernameNotFoundException("User with this id wasn't found!"));

        person.setIsDeleted(true);
        peopleRepository.saveAndFlush(person);
    }
}
