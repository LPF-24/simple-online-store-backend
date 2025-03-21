package com.simple_online_store_backend.dto.address;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class AddressResponseDTO {
    private String city;

    private String street;

    private String houseNumber;

    private String postalCode;
}
