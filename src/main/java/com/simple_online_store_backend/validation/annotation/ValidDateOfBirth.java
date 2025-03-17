package com.simple_online_store_backend.validation.annotation;

import com.simple_online_store_backend.validation.validator.DateOfBirthConstraintValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = DateOfBirthConstraintValidator.class)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDateOfBirth {
    String message() default "Date of birth must be in the past. User must be over 14 years old.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
