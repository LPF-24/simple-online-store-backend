package com.simple_online_store_backend.validation.validator;

import com.simple_online_store_backend.dto.address.AddressRequestDTO;
import com.simple_online_store_backend.validation.annotation.ValidPostalCode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PostalCodeValidator implements ConstraintValidator<ValidPostalCode, AddressRequestDTO> {
    @Override
    public boolean isValid(AddressRequestDTO dto, ConstraintValidatorContext context) {
        if (dto == null || dto.getDeliveryType() == null) return true;

        boolean isPostal = "POSTAL".equalsIgnoreCase(dto.getDeliveryType());

        if (isPostal && (dto.getPostalCode() == null || dto.getPostalCode().isBlank())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Postal code is required for postal delivery")
                    .addPropertyNode("postalCode")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
