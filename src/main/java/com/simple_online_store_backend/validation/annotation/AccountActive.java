package com.simple_online_store_backend.validation.annotation;

import com.simple_online_store_backend.validation.validator.AccountActiveValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AccountActiveValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AccountActive {
    String message() default "Your account must be active to place an order";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
