package com.simple_online_store_backend.validation.validator;

import com.simple_online_store_backend.dto.address.AddressRequestDTO;
import com.simple_online_store_backend.enums.HousingType;
import com.simple_online_store_backend.validation.annotation.ValidApartment;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ApartmentValidator implements ConstraintValidator<ValidApartment, AddressRequestDTO> {
    @Override
    public boolean isValid(AddressRequestDTO dto, ConstraintValidatorContext context) {
        if (dto.getHousingType() == HousingType.APARTMENT) {
            if (dto.getApartment() == null || dto.getApartment().isBlank()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Apartment is required when housing type is APARTMENT")
                        .addPropertyNode("apartment")
                        .addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}
