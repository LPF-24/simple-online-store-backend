package com.simple_online_store_backend.dto.address;

import com.simple_online_store_backend.enums.HousingType;
import com.simple_online_store_backend.validation.annotation.ValidApartment;
import com.simple_online_store_backend.validation.annotation.ValidPostalCode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@ValidApartment
@ValidPostalCode
public class AddressUpdateDTO {
    @NotEmpty(message = "City name can't be empty!")
    @Pattern(regexp = "^[A-Z][a-zA-Z \\-]{1,49}$",
            message = "City name must start with a capital letter and contain only letters, spaces, dashes or apostrophes")
    private String city;

    @Pattern(regexp = "^[A-Z0-9][a-zA-Z0-9 .\\-]{1,99}$",
    message = "Street name must start with a capital letter or number and contain only letters, numbers, spaces, dots or dashes")
    private String street;

    @NotEmpty(message = "House number name can't be empty!")
    @Pattern(regexp = "^[0-9]+[A-Za-z]?([/-][0-9]+[A-Za-z]?)?$", message = "Invalid house number format")
    private String houseNumber;

    private HousingType housingType;

    @NotNull(message = "Housing type must be specified")
    private String apartment;

    private String postalCode;
}
