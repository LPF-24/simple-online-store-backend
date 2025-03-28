package com.simple_online_store_backend.validation.validator;

import com.simple_online_store_backend.dto.order.OrderRequestDTO;
import com.simple_online_store_backend.validation.annotation.ValidPickupLocation;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PickupLocationValidator implements ConstraintValidator<ValidPickupLocation, OrderRequestDTO> {
    @Override
    public boolean isValid(OrderRequestDTO dto, ConstraintValidatorContext context) {
        if (dto == null)
            return true;

        if (!dto.getPickupLocation().getActive()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The order pick-up location must be active " +
                    "(i.e. not closed for repairs or liquidated)")
                    .addPropertyNode("pickupLocation")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
