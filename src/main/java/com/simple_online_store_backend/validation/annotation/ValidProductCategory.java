package com.simple_online_store_backend.validation.annotation;

import com.simple_online_store_backend.validation.validator.ProductCategoryValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ProductCategoryValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidProductCategory {
    String message() default "Invalid product category";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
