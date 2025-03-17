package com.simple_online_store_backend.validation.validator;

import com.simple_online_store_backend.validation.annotation.ValidDateOfBirth;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class DateOfBirthConstraintValidator implements ConstraintValidator<ValidDateOfBirth, LocalDate> {
    @Override
    public boolean isValid(LocalDate dateOfBirth, ConstraintValidatorContext constraintValidatorContext) {
        System.out.println("Method isValid of DateOfBirthConstraintValidator started");
        if (dateOfBirth == null) {
            return true;
        }
        System.out.println("Middle of the method isValid of DateOfBirthConstraintValidator");

        LocalDate today = LocalDate.now();
        LocalDate minDate = today.minusYears(14);
        System.out.println("Method isValid of DateOfBirthConstraintValidator ended");
        return dateOfBirth.isBefore(today) && dateOfBirth.isBefore(minDate);
    }
}
