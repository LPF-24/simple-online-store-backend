package com.simple_online_store_backend.dto.address;


import com.simple_online_store_backend.enums.DeliveryType;
import com.simple_online_store_backend.enums.HousingType;
import com.simple_online_store_backend.util.SwaggerConstants;
import com.simple_online_store_backend.validation.annotation.ValidApartment;
import com.simple_online_store_backend.validation.annotation.ValidPostalCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@ValidApartment // Apply the annotation to the entire class to access all fields at once (e.g., housingType and apartment)
@ValidPostalCode
public class AddressRequestDTO {
    @Schema(description = SwaggerConstants.CITY_DESC, example = SwaggerConstants.CITY_EXAMPLE)
    @NotEmpty(message = "City name can't be empty!")
    @Pattern(regexp = "^[A-Z][a-zA-Z \\-']{1,49}$",
            message = "City name must start with a capital letter and contain only letters, spaces, dashes or apostrophes")
    /*
     Breakdown of the regex:
     ^ and $         — anchors marking the start and end of the string.
     [A-Z]           — the first character must be an uppercase Latin letter.
     [a-zA-Z \\-']   — the rest may include letters, spaces, dashes, and apostrophes.
     {1,49}          — from 1 to 49 additional characters.
     → Total length: 2 to 50 characters.
    */
    private String city;

    @Schema(description = SwaggerConstants.STREET_DESC, example = SwaggerConstants.STREET_EXAMPLE)
    @NotEmpty(message = "Street name can't be empty!")
    @Pattern(regexp = "^[A-Z0-9][a-zA-Z0-9 .\\-]{1,99}$",
            message = "Street name must start with a capital letter or number and contain only letters, numbers, spaces, dots or dashes")
    private String street;

    @Schema(description = SwaggerConstants.HOUSE_NUMBER_DESC, example = SwaggerConstants.HOUSE_NUMBER_EXAMPLE)
    @NotEmpty(message = "House number name can't be empty!")
    @Pattern(regexp = "^[0-9]+[A-Za-z]?([/-][0-9]+[A-Za-z]?)?$", message = "Invalid house number format")
    private String houseNumber;

    @Schema(description = SwaggerConstants.HOUSING_TYPE_DESC, example = SwaggerConstants.HOUSING_TYPE_EXAMPLE)
    private HousingType housingType;

    @Schema(description = SwaggerConstants.APARTMENT_DESC, example = SwaggerConstants.APARTMENT_EXAMPLE)
    @NotNull(message = "Housing type must be specified")
    private String apartment;

    @Schema(description = SwaggerConstants.POSTAL_CODE_DESC, example = SwaggerConstants.POSTAL_CODE_EXAMPLE)
    private String postalCode;

    @NotNull(message = "Delivery type must be specified")
    @Schema(example = "POSTAL")
    private DeliveryType deliveryType;
}
