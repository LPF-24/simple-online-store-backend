package com.simple_online_store_backend.util;

public class SwaggerConstants {
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

    public static final String ID_DESC = "Unique identifier of the person";
    public static final String ID_EXAMPLE = "42";
}
