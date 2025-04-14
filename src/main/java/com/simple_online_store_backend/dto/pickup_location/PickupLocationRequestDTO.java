package com.simple_online_store_backend.dto.pickup_location;

import com.simple_online_store_backend.util.SwaggerConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class PickupLocationRequestDTO {
    @Schema(description = SwaggerConstants.CITY_DESC, example = SwaggerConstants.CITY_EXAMPLE)
    @NotEmpty(message = "City name can't be empty!")
    @Pattern(regexp = "^[A-Z][a-zA-Z \\-']{1,49}$",
            message = "City name must start with a capital letter and contain only letters, spaces, dashes or apostrophes")
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
}
