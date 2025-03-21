package com.simple_online_store_backend.validation.annotation;

import com.simple_online_store_backend.validation.validator.PostalCodeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PostalCodeValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPostalCode {
    String message() default "Postal code is required for postal delivery";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
