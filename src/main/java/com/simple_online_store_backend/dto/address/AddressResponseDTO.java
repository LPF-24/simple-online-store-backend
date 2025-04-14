package com.simple_online_store_backend.dto.address;

import com.simple_online_store_backend.util.SwaggerConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class AddressResponseDTO {
    @Schema(description = SwaggerConstants.CITY_DESC, example = SwaggerConstants.CITY_EXAMPLE)
    private String city;

    @Schema(description = SwaggerConstants.STREET_DESC, example = SwaggerConstants.STREET_EXAMPLE)
    private String street;

    @Schema(description = SwaggerConstants.HOUSE_NUMBER_DESC, example = SwaggerConstants.HOUSE_NUMBER_EXAMPLE)
    private String houseNumber;

    @Schema(description = SwaggerConstants.POSTAL_CODE_DESC, example = SwaggerConstants.POSTAL_CODE_EXAMPLE)
    private String postalCode;

    @Schema(description = SwaggerConstants.APARTMENT_DESC, example = SwaggerConstants.APARTMENT_EXAMPLE)
    private String apartment;
}
