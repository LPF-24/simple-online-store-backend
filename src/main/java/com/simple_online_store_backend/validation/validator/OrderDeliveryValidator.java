package com.simple_online_store_backend.validation.validator;

import com.simple_online_store_backend.dto.order.OrderCreateRequest;
import com.simple_online_store_backend.validation.annotation.ValidDeliveryOption;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class OrderDeliveryValidator implements ConstraintValidator<ValidDeliveryOption, OrderCreateRequest> {

    @Override
    public boolean isValid(OrderCreateRequest dto, ConstraintValidatorContext context) {
        if (dto == null) return true;

        Integer addressId = dto.getAddressId();
        Integer pickupLocationId = dto.getPickupLocationId();

        boolean hasAddress = addressId != null;
        boolean hasPickup  = pickupLocationId != null;

        if (hasAddress ^ hasPickup) {
            return true;
        }

        context.disableDefaultConstraintViolation();

        if (hasAddress && hasPickup) {
            context.buildConstraintViolationWithTemplate(
                    "Cannot use both delivery types at once — choose only one"
            ).addPropertyNode("addressId").addConstraintViolation();

            context.buildConstraintViolationWithTemplate(
                    "Cannot use both delivery types at once — choose only one"
            ).addPropertyNode("pickupLocationId").addConstraintViolation();
        } else {
            context.buildConstraintViolationWithTemplate(
                    "One of the delivery options is required: addressId or pickupLocationId"
            ).addPropertyNode("addressId").addConstraintViolation();

            context.buildConstraintViolationWithTemplate(
                    "One of the delivery options is required: addressId or pickupLocationId"
            ).addPropertyNode("pickupLocationId").addConstraintViolation();
        }

        return false;
    }
}