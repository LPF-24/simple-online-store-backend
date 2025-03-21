package com.simple_online_store_backend.dto.address;


import com.simple_online_store_backend.enums.HousingType;
import com.simple_online_store_backend.validation.annotation.ValidApartment;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@ValidApartment //вешаем её на весь класс, чтобы сразу иметь доступ ко всем полям (housingType и apartment)
public class AddressRequestDTO {
    @NotEmpty(message = "City name can't be empty!")
    @Pattern(regexp = "^[A-Z][a-zA-Z \\-']{1,49}$",
            message = "City name must start with a capital letter and contain only letters, spaces, dashes or apostrophes")
    /*Расшифровка:
    ^ и $ — начало и конец строки.
    [A-ZА-ЯЁ] — первая буква заглавная (латинская или кириллическая).
    [a-zа-яёA-ZА-ЯЁ \-']{1,49} — остальные символы: буквы, пробелы, дефисы и апострофы, от 1 до 49 символов.
    итого: от 2 до 50 символов.*/
    private String city;

    @NotEmpty(message = "Street name can't be empty!")
    @Pattern(regexp = "^[A-Za-z][a-zA-Z0-9 .\\-]{1,99}$",
            message = "Street name must start with a capital letter and contain only letters, numbers, spaces, dots or dashes")
    private String street;

    @NotEmpty(message = "House number name can't be empty!")
    @Pattern(regexp = "^[0-9]+[A-Za-z]?([/-][0-9]+[A-Za-z]?)?$", message = "Invalid house number format")
    private String houseNumber;

    private HousingType housingType;

    @NotNull(message = "Housing type must be specified")
    private String apartment;

    private String postalCode;

    private String deliveryType;
}
