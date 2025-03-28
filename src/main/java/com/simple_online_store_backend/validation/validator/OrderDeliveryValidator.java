package com.simple_online_store_backend.validation.validator;

import com.simple_online_store_backend.dto.order.OrderRequestDTO;
import com.simple_online_store_backend.validation.annotation.ValidDeliveryOption;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class OrderDeliveryValidator implements ConstraintValidator<ValidDeliveryOption, OrderRequestDTO> {
    @Override
    public boolean isValid(OrderRequestDTO dto, ConstraintValidatorContext context) {
        if (dto.getPickupLocation() != null && dto.getAddress() != null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Cannot use both delivery types at once — choose only one"
            ).addPropertyNode("pickupLocation").addConstraintViolation();

            context.buildConstraintViolationWithTemplate(
                    "Cannot use both delivery types at once — choose only one"
            ).addPropertyNode("address").addConstraintViolation();
            return false;
        }

        return true;
    }
}
