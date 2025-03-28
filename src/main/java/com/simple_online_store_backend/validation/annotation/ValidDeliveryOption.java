package com.simple_online_store_backend.validation.annotation;

import com.simple_online_store_backend.validation.validator.OrderDeliveryValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = OrderDeliveryValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDeliveryOption {
    String message() default "Invalid delivery type";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
