package com.simple_online_store_backend.util;

import com.simple_online_store_backend.dto.PersonRequestDTO;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.service.PersonDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@RequiredArgsConstructor
public class PersonValidator implements Validator {
    private final PersonDetailsService personDetailsService;

    @Override
    public boolean supports(Class<?> clazz) {
        return PersonRequestDTO.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        //Person person
    }
}
