package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.dto.order.*;
import com.simple_online_store_backend.exception.ErrorResponseDTO;
import com.simple_online_store_backend.exception.ErrorUtil;
import com.simple_online_store_backend.service.AdminService;
import com.simple_online_store_backend.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Order", description = "Endpoints for work with orders")
@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;
    private final AdminService adminService;

    @Autowired
    public OrderController(OrderService orderService, AdminService adminService) {
        this.orderService = orderService;
        this.adminService = adminService;
    }

    @Operation(
            summary = "Get all orders of the authenticated user",
            description = """
                    Returns the list of orders that belong to the currently authenticated user.
            
                    ### How to test in Swagger UI
            
                    **200 OK (success, non-empty):**
                    1) `POST /auth/login` as a user with `ROLE_USER` → copy `token`.
                    2) Click **Authorize** → paste `Bearer <token>`.
                    3) `POST /orders/create-order` → create orders
                    4) `GET /orders/all-my-orders` → **200 OK** and a JSON array of your orders.
            
                    **200 OK (empty list):**
                    - Login as a `ROLE_USER` who has no orders yet → call the endpoint → you'll get an empty array `[]`.
            
                    **401 UNAUTHORIZED:**
                    - Click **Authorize → Logout** (remove token) **or** paste a broken token like `Bearer abc.def.ghi` → call the endpoint → **401**.
                    - If the token is expired, you'll also get **401** (`TOKEN_EXPIRED`).
            
                    **403 FORBIDDEN (not enough authority):**
                    - Login as `ROLE_ADMIN` (if access is restricted to regular users) → call the endpoint → **403 ACCESS_DENIED**.
            
                    **423 ACCOUNT_LOCKED:**
                    1) Login as a normal user → Authorize.
                    2) `POST /auth/dev/_lock?username=<that_user>` (dev only).
                    3) Call this endpoint → **423**.
                    4) To restore → `POST /auth/dev/_unlock?username=<that_user>`.
            
                    **Notes:**
                    - Requires a valid JWT access token (bearer auth).
                    - Dev endpoints are available only when `demo.helpers.enabled=true`.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User's orders",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = OrderResponseDTO.class)),
                            examples = {
                                    @ExampleObject(
                                            name = "Non-empty list",
                                            summary = "Two orders",
                                            value = """
                                                    [
                                                      { "id": 101, "status": "PENDING",   "createdAt": "2025-01-10T12:30:00Z" },
                                                      { "id": 102, "status": "CANCELLED", "createdAt": "2025-01-12T09:05:00Z" }
                                                    ]
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Empty list",
                                            summary = "User has no orders yet",
                                            value = "[]"
                                    )})),

            @ApiResponse(responseCode = "401", description = "Unauthorized (no/invalid/expired token)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "UNAUTHORIZED",
                                            summary = "No token provided",
                                            value = """
                                                    {
                                                      "status": 401,
                                                      "code": "UNAUTHORIZED",
                                                      "message": "Authentication is required to access this resource",
                                                      "path": "/orders/all-my-orders"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "INVALID_ACCESS_TOKEN",
                                            summary = "Broken token",
                                            value = """
                                                    {
                                                      "status": 401,
                                                      "code": "INVALID_ACCESS_TOKEN",
                                                      "message": "Invalid access token",
                                                      "path": "/orders/all-my-orders"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "TOKEN_EXPIRED",
                                            summary = "Expired access token",
                                            value = """
                                                    {
                                                      "status": 401,
                                                      "code": "TOKEN_EXPIRED",
                                                      "message": "The access token has expired",
                                                      "path": "/orders/all-my-orders"
                                                    }
                                                    """
                                    )})),

            @ApiResponse(responseCode = "403", description = "Forbidden (authenticated, lacks authority)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "ACCESS_DENIED",
                                    summary = "Role does not allow this operation",
                                    value = """
                                            {
                                              "status": 403,
                                              "code": "ACCESS_DENIED",
                                              "message": "Access is denied",
                                              "path": "/orders/all-my-orders"
                                            }
                                            """
                            ))),

            @ApiResponse(responseCode = "423", description = "Account is locked/deactivated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "ACCOUNT_LOCKED",
                                    summary = "LockedException mapping from security filter",
                                    value = """
                                            {
                                              "status": 423,
                                              "code": "ACCOUNT_LOCKED",
                                              "message": "Your account is deactivated. Would you like to restore it?",
                                              "path": "/orders/all-my-orders"
                                            }
                                            """))),

            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "INTERNAL_ERROR",
                                    summary = "Unhandled exception",
                                    value = """
                                            {
                                              "status": 500,
                                              "code": "INTERNAL_ERROR",
                                              "message": "Internal server error",
                                              "path": "/orders/all-my-orders"
                                            }
                                            """
                            )))
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/all-my-orders")
    public ResponseEntity<List<OrderListItemResponse>> allOrdersByCustomer() {
        List<OrderListItemResponse> list = orderService.findAllOrdersByCustomer();
        return ResponseEntity.ok(list);
    }

    @Operation(
            summary = "Get order by id",
            description = """
        Returns a single order by its identifier.

        **Access:** `ROLE_USER` can view only own orders; `ROLE_ADMIN` can view any order.

        ### How to test in Swagger UI

        **200 OK (owner):**
        1) `POST /auth/login` as a user with `ROLE_USER` → copy `token`.
        2) Click **Authorize** → paste `Bearer <token>`.
        3) `POST /orders/create-order` → create order with **valid JSON**
        4) Copy the ID of the created order
        5) `GET /orders/{id}` where `{id}` belongs to this user → **200 OK** with the order JSON.

        **200 OK (admin):**
        - Login as `ROLE_ADMIN`, authorize, call `GET /orders/{id}` for any existing order → **200 OK**.

        **403 FORBIDDEN (foreign order):**
        - Login as a `ROLE_USER`, authorize, call `GET /orders/{id}` that belongs to another user → **403 ACCESS_DENIED**.

        **404 NOT_FOUND:**
        - Call `GET /orders/{id}` with a non-existing id (e.g., 999999) → **404**.

        **401 UNAUTHORIZED:**
        - Click **Authorize → Logout** (no token) or paste a broken token like `Bearer abc.def.ghi` → **401**.
        - Expired tokens also result in **401** (`TOKEN_EXPIRED`).

        **423 ACCOUNT_LOCKED:**
        1) Login as a normal user → Authorize.
        2) `POST /auth/dev/_lock?username=<that_user>` (dev only).
        3) Call `GET /orders/{id}` → **423**.
        4) To restore → `POST /auth/dev/_unlock?username=<that_user>`.

        **Notes:**
        - Requires a valid JWT access token (bearer auth).
        - Dev endpoints are available only when `demo.helpers.enabled=true`.
        """
    )
    @Parameter(
            name = "id",
            description = "Order identifier",
            required = true,
            example = "1",
            schema = @Schema(type = "integer", format = "int32", minimum = "1")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OrderResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "OK",
                                    summary = "Order payload",
                                    value = """
                        {
                          "id": 1,
                          "status": "PENDING",
                          "createdAt": "2025-01-10T12:30:00Z"
                        }
                        """
                            ))),

            @ApiResponse(responseCode = "401", description = "Unauthorized (no/invalid/expired token)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "UNAUTHORIZED",
                                            summary = "No token provided",
                                            value = """
                            {
                              "status": 401,
                              "code": "UNAUTHORIZED",
                              "message": "Authentication is required to access this resource",
                              "path": "/orders/1"
                            }
                            """
                                    ),
                                    @ExampleObject(
                                            name = "INVALID_ACCESS_TOKEN",
                                            summary = "Broken token",
                                            value = """
                            {
                              "status": 401,
                              "code": "INVALID_ACCESS_TOKEN",
                              "message": "Invalid access token",
                              "path": "/orders/1"
                            }
                            """
                                    ),
                                    @ExampleObject(
                                            name = "TOKEN_EXPIRED",
                                            summary = "Expired access token",
                                            value = """
                            {
                              "status": 401,
                              "code": "TOKEN_EXPIRED",
                              "message": "The access token has expired",
                              "path": "/orders/1"
                            }
                            """
                                    )
                            }
                    )),

            @ApiResponse(responseCode = "403", description = "Forbidden (authenticated, lacks authority or not the owner)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "ACCESS_DENIED",
                                    summary = "Role or ownership check failed",
                                    value = """
                        {
                          "status": 403,
                          "code": "ACCESS_DENIED",
                          "message": "Access is denied",
                          "path": "/orders/101"
                        }
                        """
                            ))
            ),

            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "ENTITY_NOT_FOUND",
                                    summary = "Order id does not exist",
                                    value = """
                        {
                          "status": 404,
                          "code": "ENTITY_NOT_FOUND",
                          "message": "Order not found",
                          "path": "/orders/999999"
                        }
                        """
                            ))
            ),

            @ApiResponse(responseCode = "423", description = "Account is locked/deactivated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "ACCOUNT_LOCKED",
                                    summary = "LockedException mapping from security filter",
                                    value = """
                        {
                          "status": 423,
                          "code": "ACCOUNT_LOCKED",
                          "message": "Your account is deactivated. Would you like to restore it?",
                          "path": "/orders/101"
                        }
                        """
                            ))
            ),

            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "INTERNAL_ERROR",
                                    summary = "Unhandled exception",
                                    value = """
                        {
                          "status": 500,
                          "code": "INTERNAL_ERROR",
                          "message": "Internal server error",
                          "path": "/orders/101"
                        }
                        """
                            ))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailsResponse> getOrder(@PathVariable("id") int orderId) {
        OrderDetailsResponse dto = orderService.getOrderById(orderId);
        return ResponseEntity.ok(dto);
    }

    @Operation(
            summary = "Create a new order",
            description = """
            Creates a new order for the authenticated user.

            ### How to test in Swagger UI

            **200 OK (success):**
            1. `POST /auth/login` as `ROLE_USER` → copy `token`.
            2. Click **Authorize** → `Bearer <token>`.
            3. `POST /orders/create-order` with a valid JSON (see examples) → you'll get order with status `PENDING`.

            **400 VALIDATION_ERROR / MESSAGE_NOT_READABLE:**
            - Send invalid/incomplete JSON or both addressId & pickupLocationId → `400`.

            **401 UNAUTHORIZED:**
            - No token / broken token → `401`.

            **403 FORBIDDEN:**
            - If your security forbids admins to create orders, login as `ROLE_ADMIN` → `403`.
            
            **404 ENTITY_NOT_FOUND:**
            - Use a non-existing `addressId` (e.g., 1073741824) with valid `productIds` → 404.
            - Or use a non-existing `pickupLocationId` (e.g., 1073741824) with valid `productIds` → 404.

            **423 ACCOUNT_LOCKED:**
            1. Login as user → Authorize → `POST /auth/dev/_lock?username=<user>`.
            2. Call this endpoint → `423`.
            3. To restore → `POST /auth/dev/_unlock?username=<user>`.

            **Notes:**
            - Exactly one of `addressId` or `pickupLocationId` must be provided.
            - All products must exist and be available.
            - Returns a detailed order payload.
            """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Order creation payload. Exactly one of `addressId` or `pickupLocationId` must be provided.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = OrderCreateRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "Valid: Home delivery",
                                    summary = "Products exist; deliver to an existing address",
                                    value = """
                                            {
                                              "productIds": [1, 2],
                                              "addressId": 1
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Valid: Pickup",
                                    summary = "Products exist; pickup from an active location",
                                    value = """
                                            {
                                              "productIds": [1],
                                              "pickupLocationId": 1
                                            }
                                            """
                            ),

                            @ExampleObject(
                                    name = "Invalid: No products",
                                    summary = "Fails @NotEmpty on productIds",
                                    value = """
                                            {
                                              "productIds": [],
                                              "addressId": 1
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Invalid: Both addressId and pickupLocationId",
                                    summary = "Mutually exclusive: provide exactly one target",
                                    value = """
                                            {
                                              "productIds": [1, 2],
                                              "addressId": 1,
                                              "pickupLocationId": 1
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Invalid: Unknown productId",
                                    summary = "Product with given id does not exist",
                                    value = """
                                            {
                                              "productIds": [1073741824],
                                              "addressId": 1
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Invalid: Unknown addressId",
                                    summary = "Address not found",
                                    value = """
                                            {
                                              "productIds": [1, 2],
                                              "addressId": 1073741824
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Invalid: Unknown pickupLocationId",
                                    summary = "Pickup location not found",
                                    value = """
                                            {
                                              "productIds": [1],
                                              "pickupLocationId": 1073741824
                                            }
                                            """
                            ),

                            // === MALFORMED JSON → 400 MESSAGE_NOT_READABLE ===
                            @ExampleObject(
                                    name = "Malformed JSON",
                                    summary = "Broken JSON body (will trigger MESSAGE_NOT_READABLE)",
                                    value = """
                                            {
                                              "productIds": [1, 2],
                                              "addressId": 1
                                            """
                            )
                    }
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order successfully created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OrderDetailsResponse.class),
                            examples = @ExampleObject(
                                    name = "OK (home delivery)",
                                    value = """
                                            {
                                              "id": 101,
                                              "status": "PENDING",
                                              "ownerId": 2,
                                              "ownerUserName": "maria",
                                              "address": {
                                                "city": "Berlin",
                                                "street": "Main Street",
                                                "houseNumber": "12A",
                                                "apartment": "45",
                                                "postalCode": "10115"
                                              },
                                              "pickup": null,
                                              "items": [
                                                { "productId": 1, "productName": "Phone", "price": 499.99, "quantity": 1 },
                                                { "productId": 2, "productName": "Case",  "price": 19.99,  "quantity": 1 }
                                              ]
                                            }
                                            """
                            ))),
            @ApiResponse(responseCode = "400", description = "Validation failed / malformed JSON",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = {
                                    @ExampleObject(name = "VALIDATION_ERROR (no products)", value = """
                                    {
                                      "status": 400,
                                      "code": "VALIDATION_ERROR",
                                      "message": "You must add at least one product",
                                      "path": "/orders/create-order"
                                    }"""),
                                    @ExampleObject(name = "VALIDATION_ERROR (both address & pickup)", value = """
                                    {
                                      "status": 400,
                                      "code": "VALIDATION_ERROR",
                                      "message": "Either addressId or pickupLocationId must be provided (but not both)",
                                      "path": "/orders/create-order"
                                    }"""),
                                    @ExampleObject(name = "MESSAGE_NOT_READABLE", value = """
                                    {
                                      "status": 400,
                                      "code": "MESSAGE_NOT_READABLE",
                                      "message": "JSON parse error: Unexpected end-of-input",
                                      "path": "/orders/create-order"
                                    }""")
                            })),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Entity not found (address or pickup location)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "ENTITY_NOT_FOUND (addressId)",
                                            summary = "Address not found by id",
                                            value = """
                                                    {
                                                      "status": 404,
                                                      "code": "ENTITY_NOT_FOUND",
                                                      "message": "Address not found: 1073741824",
                                                      "path": "/orders/create-order"
                                                    }
                                                    """),
                                    @ExampleObject(
                                            name = "ENTITY_NOT_FOUND (pickupLocationId)",
                                            summary = "Pickup location not found by id",
                                            value = """
                                                    {
                                                      "status": 404,
                                                      "code": "ENTITY_NOT_FOUND",
                                                      "message": "Pickup location not found: 1073741824",
                                                      "path": "/orders/create-order"
                                                    }
                                                    """)})),
            @ApiResponse(responseCode = "423", description = "Account locked/deactivated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/create-order")
    public ResponseEntity<OrderDetailsResponse> addOrder(@RequestBody @Valid OrderCreateRequest req, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            ErrorUtil.returnErrorsToClient(bindingResult);
        }
        OrderDetailsResponse response = orderService.createOrder(req);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get all orders (admin)",
            description = """
        Returns a lightweight list of all orders for administrators.

        Each item contains only: `id`, `status`, `productCount`.

        ### How to test in Swagger UI

        **200 OK (success):**
        1. `POST /auth/login` as a user with `ROLE_ADMIN` → copy `token`.
        2. Click **Authorize** → `Bearer <token>`.
        3. `GET /orders` → you'll get an array of items.

        **401 UNAUTHORIZED:**
        - No token / malformed token / expired token → `401` (see response schema below).

        **403 FORBIDDEN:**
        - Logged in without `ROLE_ADMIN` (e.g., `ROLE_USER`) → `403`.

        **Notes:**
        - Endpoint is admin-only.
        - Response is a *list* of lightweight items, not full order details.
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of orders (lightweight)",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = OrderListItemResponse.class)),
                            examples = @ExampleObject(
                                    name = "OK",
                                    value = """
                                        [
                                          { "id": 101, "status": "PENDING",   "productCount": 2 },
                                          { "id": 102, "status": "SHIPPED",   "productCount": 1 },
                                          { "id": 103, "status": "CANCELLED", "productCount": 3 }
                                        ]
                                        """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = {
                                    @ExampleObject(name = "TOKEN_EXPIRED", value = """
                                {
                                  "status": 401,
                                  "code": "TOKEN_EXPIRED",
                                  "message": "The refresh token has expired.",
                                  "path": "/orders"
                                }"""),
                                    @ExampleObject(name = "INVALID_REFRESH_TOKEN", value = """
                                {
                                  "status": 401,
                                  "code": "INVALID_REFRESH_TOKEN",
                                  "message": "Invalid refresh token",
                                  "path": "/orders"
                                }""")
                            }
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "ACCESS_DENIED", value = """
                        {
                          "status": 403,
                          "code": "ACCESS_DENIED",
                          "message": "Access is denied",
                          "path": "/orders"
                        }""")
                    )
            ),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "INTERNAL_ERROR", value = """
                        {
                          "status": 500,
                          "code": "INTERNAL_ERROR",
                          "message": "Internal server error",
                          "path": "/orders"
                        }""")
                    )
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping()
    public ResponseEntity<List<OrderListItemResponse>> getAllOrders() {
        return ResponseEntity.ok(adminService.findAllOrders());
    }

    @Operation(summary = "Cancel an order", description = "Changes the order status to CANCELLED")
    @ApiResponse(responseCode = "200", description = "Order successfully cancelled")
    @ApiResponse(responseCode = "500", description = "Error inside method")
    @ApiResponse(responseCode = "403", description = "User is authenticated but not allowed to access this resource")
    @SecurityRequirement(name = "bearerAuth")
    @RequestMapping(value = "/{id}/cancel-order", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<OrderResponseDTO> cancelOrder(@PathVariable("id") int orderId) {
        OrderResponseDTO response = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reactivate an order", description = "Changes the order status to PENDING")
    @ApiResponse(responseCode = "200", description = "Order successfully reactivated")
    @ApiResponse(responseCode = "500", description = "Error inside method")
    @ApiResponse(responseCode = "403", description = "User is authenticated but not allowed to access this resource")
    @SecurityRequirement(name = "bearerAuth")
    @RequestMapping(value = "/{id}/reactivate-order", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<OrderResponseDTO> reactivateOrder(@PathVariable("id") int orderId) {
        OrderResponseDTO response = orderService.reactivateOrder(orderId);
        return ResponseEntity.ok(response);
    }
}
