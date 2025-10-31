package com.simple_online_store_backend.validation.validator;

import com.simple_online_store_backend.entity.PickupLocation;
import com.simple_online_store_backend.repository.PickupLocationRepository;
import com.simple_online_store_backend.validation.annotation.ValidPickupLocation;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Optional;

public class PickupLocationValidator implements ConstraintValidator<ValidPickupLocation, PickupLocation> {
    private final PickupLocationRepository pickupLocationRepository;

    public PickupLocationValidator(PickupLocationRepository pickupLocationRepository) {
        this.pickupLocationRepository = pickupLocationRepository;
    }

    @Override
    public boolean isValid(PickupLocation location, ConstraintValidatorContext context) {
        if (location == null)
            return true;

        Optional<PickupLocation> optional = pickupLocationRepository.findById(location.getId());

        if (optional.isEmpty() || !Boolean.TRUE.equals(optional.get().getActive())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "The order pick-up location must be active (i.e. not closed for repairs or liquidated)"
                    ).addConstraintViolation();
            return false;
        }

        return true;
    }
}
