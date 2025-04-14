package com.simple_online_store_backend.util;

public class SwaggerConstants {
    // Common constants for several entities.
    public static final String ID_DESC = "Unique identifier of the ";
    public static final String ID_EXAMPLE = "42";

    // Constants for classes related to the Person entity.
    public static final String USERNAME_DESC = "Username of the person";
    public static final String USERNAME_EXAMPLE = "john_doe";

    public static final String PASSWORD_DESC = "Password (min 8 characters, max 30 characters, " +
            "the first letter is capitalized, contains one digit, one special character and does not contain spaces)";
    public static final String PASSWORD_EXAMPLE = "Test234!";

    public static final String DATE_OF_BIRTH_DESC = "Date of birth (the minimum number of full years for registration is 14)";
    public static final String DATE_OF_BIRTH_EXAMPLE = "2000-01-01";

    public static final String PHONE_NUMBER_DESC = "Phone number (starting with +, min 7 characters, max 20 characters";
    public static final String PHONE_NUMBER_EXAMPLE = "+12345678";

    public static final String EMAIL_DESC = "Email address";
    public static final String EMAIL_EXAMPLE = "john@example.com";

    public static final String AGREEMENT_ACCEPTED_DESC = "Necessary to verify that the user has agreed consent";

    public static final String SPECIAL_CODE_DESC = "Required to register a user as an admin";

    // Constants for classes related to the PickupLocation and Address entities
    public static final String CITY_DESC = "The city where the order pick-up location or address is located " +
            "(starting with a capital letter and contain only letters, spaces, dashes or apostrophes)";
    public static final String CITY_EXAMPLE = "New York";

    public static final String STREET_DESC = "Street where the pick-up point is located or address";
    public static final String STREET_EXAMPLE = "Main Street";

    public static final String HOUSE_NUMBER_DESC = "The house number where the pick-up point is located or the address " +
            "(contains only numbers and letters)";
    public static final String HOUSE_NUMBER_EXAMPLE = "12A";

    // Constants for classes related to the Address entity.
    public static final String HOUSING_TYPE_DESC = "Housing type is required when creating an order (to check whether an apartment number is required).";
    public static final String HOUSING_TYPE_EXAMPLE = "APARTMENT";

    public static final String APARTMENT_DESC = "The apartment number is required if the selected housing type is APARTMENT.";
    public static final String APARTMENT_EXAMPLE = "34";

    public static final String POSTAL_CODE_DESC = "Postal code is required for postal delivery";
    public static final String POSTAL_CODE_EXAMPLE = "1234";


}
