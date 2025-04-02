package com.simple_online_store_backend.util;

import com.simple_online_store_backend.dto.person.PersonRequestDTO;
import com.simple_online_store_backend.repository.PeopleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@RequiredArgsConstructor
public class PersonValidator implements Validator {
    private final PeopleRepository peopleRepository;
    private static final Logger logger = LoggerFactory.getLogger(PersonValidator.class);

    @Override
    public boolean supports(Class<?> clazz) {
        return PersonRequestDTO.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        logger.info("Method validate of PersonValidator started");
        PersonRequestDTO dto = (PersonRequestDTO) target;

        logger.info("Middle of the method validate of PersonValidator");
        if (peopleRepository.findByUserName(dto.getUserName()).isPresent()) {
            logger.error("Person with username is already existed");
            errors.rejectValue("userName", "Person with this username is already existed!");
        }

        logger.info("Method validate of PersonValidator ended");
    }
}
