package com.simple_online_store_backend.dto.pickup_location;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class PickupLocationResponseDTO {
    private Integer id;

    private String city;

    private String street;

    private String houseNumber;
}
