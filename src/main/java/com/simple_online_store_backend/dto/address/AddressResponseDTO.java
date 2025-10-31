package com.simple_online_store_backend.dto.address;

import com.simple_online_store_backend.util.SwaggerConstants;
import io.swagger.v3.oas.annotations.media.Schema;

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

    public AddressResponseDTO() {
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public void setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getApartment() {
        return apartment;
    }

    public void setApartment(String apartment) {
        this.apartment = apartment;
    }
}
