package com.simple_online_store_backend.validation.annotation;

import com.simple_online_store_backend.validation.validator.PickupLocationValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PickupLocationValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPickupLocation {
    String message() default "Invalid pick up location";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
