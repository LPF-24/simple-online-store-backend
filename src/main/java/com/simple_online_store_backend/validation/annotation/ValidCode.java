package com.simple_online_store_backend.validation.annotation;

import com.simple_online_store_backend.validation.validator.CodeConstraintValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CodeConstraintValidator.class)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCode {
    String message() default "Invalid code!";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
