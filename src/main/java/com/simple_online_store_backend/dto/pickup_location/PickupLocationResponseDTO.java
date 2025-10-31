package com.simple_online_store_backend.dto.pickup_location;

import com.simple_online_store_backend.util.SwaggerConstants;
import io.swagger.v3.oas.annotations.media.Schema;

public class PickupLocationResponseDTO {
    @Schema(description = SwaggerConstants.ID_DESC + " pick-up location", example = SwaggerConstants.ID_EXAMPLE)
    private Integer id;

    @Schema(description = SwaggerConstants.CITY_DESC, example = SwaggerConstants.CITY_EXAMPLE)
    private String city;

    @Schema(description = SwaggerConstants.STREET_DESC, example = SwaggerConstants.STREET_EXAMPLE)
    private String street;

    @Schema(description = SwaggerConstants.HOUSE_NUMBER_DESC, example = SwaggerConstants.HOUSE_NUMBER_EXAMPLE)
    private String houseNumber;

    public PickupLocationResponseDTO() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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
}
