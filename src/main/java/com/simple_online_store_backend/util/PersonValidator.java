package com.simple_online_store_backend.util;

import com.simple_online_store_backend.dto.PersonRequestDTO;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.mapper.PersonConverter;
import com.simple_online_store_backend.repository.PeopleRepository;
import com.simple_online_store_backend.service.PersonDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@RequiredArgsConstructor
public class PersonValidator implements Validator {
    private final PeopleRepository peopleRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return PersonRequestDTO.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        System.out.println("Method validate of PersonValidator started");
        PersonRequestDTO dto = (PersonRequestDTO) target;

        System.out.println("Middle of the method validate of PersonValidator");
        if (peopleRepository.findByUserName(dto.getUserName()).isPresent()) {
            System.out.println("Person with username is already existed");
            errors.rejectValue("userName", "Person with this username is already existed!");
        }

        System.out.println("Method validate of PersonValidator ended");
    }
}
