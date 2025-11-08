package com.simple_online_store_backend.util;

public class SwaggerConstants {
    // Common constants for several entities.
    public static final String ID_DESC = "Unique identifier of the ";
    public static final String ID_EXAMPLE = "1";

    // Constants for classes related to the Person entity.
    public static final String USERNAME_DESC = "Username of the person";
    public static final String USERNAME_EXAMPLE = "user";

    public static final String PASSWORD_DESC = "Password (min 8 characters, max 30 characters, " +
            "the first letter is capitalized, contains one digit, one special character and does not contain spaces)";
    public static final String PASSWORD_EXAMPLE = "user123!";

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

    // Constants for OrderCreateRequest
    public static final String ORDER_PRODUCT_IDS_DESC =
            "List of product IDs in the order (minimum one item, each ID >= 1)";
    public static final String ORDER_PRODUCT_IDS_EXAMPLE = "[1, 2, 3]";

    public static final String ORDER_ADDRESS_ID_DESC =
            "Delivery address ID. Please specify either addressId or pickupLocationId (if both are omitted, the request will be rejected by business validation).";
    public static final String ORDER_PICKUP_LOCATION_ID_DESC =
            "Pickup location ID. Please enter either pickupLocationId or addressId.";

    // --- For OrderDetailsResponse ---
    public static final String ORDER_STATUS_DESC =
            "Order status (enum).";
    public static final String ORDER_OWNER_ID_DESC =
            "Order owner (user) ID.";
    public static final String ORDER_OWNER_USERNAME_DESC =
            "The username of the order owner.";
    public static final String ORDER_ADDRESS_DESC =
            "Delivery address (if ordering with delivery). May be null if pickup is selected.";
    public static final String ORDER_PICKUP_DESC =
            "Pickup location (if ordering for pickup). May be null if delivery is selected.";
    public static final String ORDER_ITEMS_DESC =
            "Order items (minimum one item).";

    // --- For OrderItemResponse ---
    public static final String PRODUCT_ID_DESC =
            "Unique identifier of the product included in the order.";
    public static final String PRODUCT_NAME_DESC =
            "Name of the product.";
    public static final String PRODUCT_NAME_EXAMPLE = "Wireless Headphones";
    public static final String PRODUCT_PRICE_DESC =
            "Unit price of the product in the order.";
    public static final String PRODUCT_PRICE_EXAMPLE = "99.99";
    public static final String PRODUCT_QUANTITY_DESC =
            "Quantity of the given product within the order.";
    public static final String PRODUCT_QUANTITY_EXAMPLE = "2";

    // --- For OrderListItemResponse ---
    public static final String ORDER_PRODUCT_COUNT_DESC =
            "Number of products included in the order.";
    public static final String ORDER_PRODUCT_COUNT_EXAMPLE = "3";
}
