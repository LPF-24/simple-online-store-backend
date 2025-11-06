package com.simple_online_store_backend.validation.validator;

import com.simple_online_store_backend.enums.ProductCategory;
import com.simple_online_store_backend.validation.annotation.ValidProductCategory;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.EnumSet;

public class ProductCategoryValidator implements ConstraintValidator<ValidProductCategory, ProductCategory> {
    @Override
    public boolean isValid(ProductCategory value, ConstraintValidatorContext context) {
        if (value == null) return true;
        return java.util.EnumSet.allOf(ProductCategory.class).contains(value);
    }
}
