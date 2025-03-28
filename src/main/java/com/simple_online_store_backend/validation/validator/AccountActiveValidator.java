package com.simple_online_store_backend.validation.validator;

import com.simple_online_store_backend.dto.order.OrderRequestDTO;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.validation.annotation.AccountActive;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AccountActiveValidator implements ConstraintValidator<AccountActive, Person> {
    @Override
    public boolean isValid(Person person, ConstraintValidatorContext context) {
        return person != null && !person.getIsDeleted();
    }
}
