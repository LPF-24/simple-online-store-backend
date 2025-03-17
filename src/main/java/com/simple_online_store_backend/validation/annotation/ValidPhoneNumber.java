package com.simple_online_store_backend.validation.annotation;

import com.simple_online_store_backend.validation.validator.PhoneNumberConstraintValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PhoneNumberConstraintValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhoneNumber {
    String message() default "The phone number must contain between 7 and 20 characters!";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
