package com.simple_online_store_backend.validation.annotation;

import com.simple_online_store_backend.validation.validator.ApartmentValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ApartmentValidator.class)
@Target(ElementType.TYPE) // Allows placing the annotation at the class level
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidApartment {
    String message() default "Apartment is required for apartment housing type";
    Class<?>[] groups() default {};
    // For group validation, useful when validating specific fields based on context (e.g., during update)
    Class<? extends Payload>[] payload() default {};
    // Provides additional metadata, often used in custom validators (e.g., for logging).
}
