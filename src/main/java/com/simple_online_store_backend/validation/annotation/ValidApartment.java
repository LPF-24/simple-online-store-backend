package com.simple_online_store_backend.validation.annotation;

import com.simple_online_store_backend.validation.validator.ApartmentValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ApartmentValidator.class)
@Target(ElementType.TYPE) //можно вешать на класс целиком
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidApartment {
    String message() default "Apartment is required for apartment housing type";
    Class<?>[] groups() default {}; //для групповой валидации, если ты хочешь валидировать только
    // некоторые поля в определённой ситуации (например, при обновлении).
    Class<? extends Payload>[] payload() default {}; //payload — для дополнительной информации, часто используется
    // с кастомной логикой (например, логгировать ошибки).
}
