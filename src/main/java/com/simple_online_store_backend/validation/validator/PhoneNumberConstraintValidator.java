package com.simple_online_store_backend.validation.validator;

import com.simple_online_store_backend.validation.annotation.ValidPhoneNumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneNumberConstraintValidator implements ConstraintValidator<ValidPhoneNumber, String> {
    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        if (phoneNumber == null) {
            return true;
        }

        return phoneNumber.startsWith("+")
                && phoneNumber.matches("\\+?[0-9\\- ]{7,20}");
    }
}
