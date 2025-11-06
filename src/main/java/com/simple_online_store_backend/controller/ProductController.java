package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.dto.product.ProductRequestDTO;
import com.simple_online_store_backend.dto.product.ProductResponseDTO;
import com.simple_online_store_backend.dto.product.ProductUpdateDTO;
import com.simple_online_store_backend.exception.ErrorResponseDTO;
import com.simple_online_store_backend.exception.ErrorUtil;
import com.simple_online_store_backend.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Products", description = "Managing products")
@RestController
@RequestMapping("/product")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(
            summary = "Get all products (public)",
            description = """
    Returns a list of all products.  
    This endpoint is **public** and does **not** require authentication.

    ### How to test in Swagger UI

    **200 OK (success):**
    1. Open Swagger UI.
    2. Find `GET /product`.
    3. Click **Try it out** → **Execute** (no token required) — you will receive an array of products.

    **Notes:**
    - Endpoint is marked as `permitAll()` — works without a Bearer token.
    - The response is a list of `ProductResponseDTO` objects representing all products in the system.
    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List of products",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ProductResponseDTO.class)),
                            examples = {
                                    @ExampleObject(
                                            name = "OK — multiple items",
                                            value = """
                                        [
                                          {
                                            "id": 1,
                                            "productName": "Phone",
                                            "productDescription": "Android smartphone",
                                            "category": "SMARTPHONES",
                                            "price": 499.99,
                                            "availability": true
                                          },
                                          {
                                            "id": 2,
                                            "productName": "Case",
                                            "productDescription": "Protective case",
                                            "category": "ACCESSORIES",
                                            "price": 19.99,
                                            "availability": false
                                          }
                                        ]"""
                                    ),
                                    @ExampleObject(
                                            name = "OK — empty list",
                                            value = "[]"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "INTERNAL_ERROR",
                                    value = """
                                {
                                  "status": 500,
                                  "code": "INTERNAL_ERROR",
                                  "message": "Internal server error",
                                  "path": "/product"
                                }"""
                            )
                    )
            )
    })
    @GetMapping()
    public ResponseEntity<List<ProductResponseDTO>> allProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @Operation(
            summary = "Add a new product (admin-only)",
            description = """
    Creates a new product.

    **Access:** only users with `ROLE_ADMIN`.

    ### How to test in Swagger UI

    **200 OK (success):**
    1) POST /auth/login as `ROLE_ADMIN` → copy token.
    2) Click **Authorize** → `Bearer <token>`.
    3) POST /product/add-product with a valid JSON body (see request examples below).

    **400 BAD REQUEST:**
    - Send invalid body (empty fields / negative price) → `400 VALIDATION_ERROR`.
    - Send malformed JSON or invalid enum (e.g., wrong `category`) → `400 MESSAGE_NOT_READABLE`.

    **401 UNAUTHORIZED:**
    - No token / invalid token → `401`.

    **403 FORBIDDEN:**
    - Logged in without admin rights (e.g., `ROLE_USER`) → `403`.
    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "New product payload",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProductRequestDTO.class),
                    examples = {
                            @ExampleObject(
                                    name = "Valid (ADMIN)",
                                    summary = "Valid payload — returns 200 OK",
                                    value = """
                                {
                                  "productName": "Samsung Galaxy A152 FullHd",
                                  "productDescription": "Android smartphone",
                                  "productCategory": "SMARTPHONES",
                                  "price": 499.99,
                                  "availability": true
                                }"""
                            ),
                            @ExampleObject(
                                    name = "Invalid (violates validation)",
                                    summary = "Empty fields / negative price — returns 400 VALIDATION_ERROR",
                                    value = """
                                {
                                  "productName": "",
                                  "productDescription": "",
                                  "category": "ACCESSORIES",
                                  "price": -1,
                                  "availability": true
                                }"""
                            ),
                            @ExampleObject(
                                    name = "Malformed JSON / invalid enum",
                                    summary = "Wrong enum or broken JSON — returns 400 MESSAGE_NOT_READABLE",
                                    value = "{ \"productName\": \"Phone\", \"productDescription\": \"Android\", \"category\": \"WRONG\", \"price\": 499.99, \"availability\": true"
                            )
                    }
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Product created",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProductResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "OK",
                                    value = """
                                {
                                  "id": 101,
                                  "productName": "Phone",
                                  "productDescription": "Android smartphone",
                                  "category": "SMARTPHONES",
                                  "price": 499.99,
                                  "availability": true
                                }"""
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request (validation / parsing failed)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "VALIDATION_ERROR",
                                            value = """
                                        {
                                          "status": 400,
                                          "code": "VALIDATION_ERROR",
                                          "message": "Product name can't be empty!; Product description can't be empty!; Price must be positive!",
                                          "path": "/product/add-product"
                                        }"""
                                    ),
                                    @ExampleObject(
                                            name = "MESSAGE_NOT_READABLE",
                                            value = """
                                        {
                                          "status": 400,
                                          "code": "MESSAGE_NOT_READABLE",
                                          "message": "Cannot deserialize value...",
                                          "path": "/product/add-product"
                                        }"""
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = {
                                    @ExampleObject(name = "UNAUTHORIZED", value = """
                                {
                                  "status": 401,
                                  "code": "UNAUTHORIZED",
                                  "message": "Full authentication is required to access this resource",
                                  "path": "/product/add-product"
                                }"""),
                                    @ExampleObject(name = "TOKEN_EXPIRED", value = """
                                {
                                  "status": 401,
                                  "code": "TOKEN_EXPIRED",
                                  "message": "The refresh token has expired.",
                                  "path": "/product/add-product"
                                }""")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden (admin-only)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "ACCESS_DENIED",
                                    value = """
                                {
                                  "status": 403,
                                  "code": "ACCESS_DENIED",
                                  "message": "Access is denied",
                                  "path": "/product/add-product"
                                }"""
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "INTERNAL_ERROR",
                                    value = """
                                {
                                  "status": 500,
                                  "code": "INTERNAL_ERROR",
                                  "message": "Internal server error",
                                  "path": "/product/add-product"
                                }"""
                            )
                    )
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/add-product")
    public ResponseEntity<ProductResponseDTO> addProduct(@RequestBody @Valid ProductRequestDTO dto,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors())
            ErrorUtil.returnErrorsToClient(bindingResult);

        ProductResponseDTO responseDTO = productService.addProduct(dto);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(
            summary = "Update a product (admin-only, partial update)",
            description = """
    Partially updates an existing product by id using `ProductUpdateDTO`.
    Only non-null fields will be applied; other fields remain unchanged.
    Access restricted to `ROLE_ADMIN`.

    ### How to test in Swagger UI

    **200 OK (success):**
    1) POST /auth/login as `ROLE_ADMIN` → copy token.
    2) Click **Authorize** → `Bearer <token>`.
    3) PATCH /product/{id}/update-product with a valid JSON body (see request examples).

    **400 BAD REQUEST (validation):**
    - Send invalid values (e.g., blank `productName`, negative `price`) → `400 VALIDATION_ERROR`.
    - Send broken JSON or wrong enum value → `400 MESSAGE_NOT_READABLE`.

    **401 UNAUTHORIZED:**
    - No token / malformed token / expired token → `401`.

    **403 FORBIDDEN:**
    - Logged in without admin rights → `403`.

    **404 ENTITY_NOT_FOUND:**
    - Use a non-existing product id → `404`.

    **409 CONFLICT (duplicate):**
    - Try to update `productName` to an already existing product's name → `409 DUPLICATE_RESOURCE`.

    **Notes:**
    - Partial update: send only the fields you want to change.
    - `productCategory` is optional for PATCH; if provided, must be a valid enum.
    """
    )
    @Parameter(
            name = "id",
            description = "Product ID to update",
            example = "1",
            required = true,
            in = io.swagger.v3.oas.annotations.enums.ParameterIn.PATH
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Partial product update payload (only non-null fields will be applied)",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProductUpdateDTO.class),
                    examples = {
                            @ExampleObject(
                                    name = "Valid (partial)",
                                    summary = "Change only name and price",
                                    value = """
                                {
                                  "productName": "Phone X",
                                  "price": 549.99
                                }"""
                            ),
                            @ExampleObject(
                                    name = "Valid (full)",
                                    summary = "Change all fields at once",
                                    value = """
                                {
                                  "productName": "Phone case with a picture of a cat",
                                  "productDescription": "Protective case (new)",
                                  "productCategory": "ACCESSORIES",
                                  "price": 19.99,
                                  "availability": false
                                }"""
                            ),
                            @ExampleObject(
                                    name = "Invalid (violates validation)",
                                    summary = "Blank name and negative price → 400 VALIDATION_ERROR",
                                    value = """
                                {
                                  "productName": "",
                                  "price": -1
                                }"""
                            ),
                            @ExampleObject(
                                    name = "Malformed JSON / wrong enum",
                                    summary = "Broken JSON or invalid enum value → 400 MESSAGE_NOT_READABLE",
                                    value = "{ \"productName\": \"Phone\", \"productCategory\": \"WRONG\", \"price\": 499.99 "
                            ),
                            @ExampleObject(
                                    name = "Duplicate name",
                                    summary = "Rename to an already existing product name → 409 DUPLICATE_RESOURCE",
                                    value = """
                                {
                                  "productName": "Phone",
                                  "availability": true
                                }"""
                            )
                    }
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Product updated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProductResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "OK",
                                    value = """
                                {
                                  "id": 1,
                                  "productName": "Phone X",
                                  "productDescription": "Android smartphone",
                                  "productCategory": "SMARTPHONES",
                                  "price": 549.99,
                                  "availability": true
                                }"""
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request (validation / parsing failed)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "VALIDATION_ERROR",
                                            value = """
                                        {
                                          "status": 400,
                                          "code": "VALIDATION_ERROR",
                                          "message": "Product name must not be blank; Price must be at least 0.01",
                                          "path": "/product/1/update-product"
                                        }"""
                                    ),
                                    @ExampleObject(
                                            name = "MESSAGE_NOT_READABLE",
                                            value = """
                                        {
                                          "status": 400,
                                          "code": "MESSAGE_NOT_READABLE",
                                          "message": "Cannot deserialize value...",
                                          "path": "/product/1/update-product"
                                        }"""
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = {
                                    @ExampleObject(name = "UNAUTHORIZED", value = """
                                {
                                  "status": 401,
                                  "code": "UNAUTHORIZED",
                                  "message": "Full authentication is required to access this resource",
                                  "path": "/product/1/update-product"
                                }"""),
                                    @ExampleObject(name = "TOKEN_EXPIRED", value = """
                                {
                                  "status": 401,
                                  "code": "TOKEN_EXPIRED",
                                  "message": "The refresh token has expired.",
                                  "path": "/product/1/update-product"
                                }""")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden (admin-only)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "ACCESS_DENIED",
                                    value = """
                                {
                                  "status": 403,
                                  "code": "ACCESS_DENIED",
                                  "message": "Access is denied",
                                  "path": "/product/1/update-product"
                                }"""
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Product not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "ENTITY_NOT_FOUND",
                                    value = """
                                {
                                  "status": 404,
                                  "code": "ENTITY_NOT_FOUND",
                                  "message": "Product with id 1073741824 wasn't found",
                                  "path": "/product/1073741824/update-product"
                                }"""
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict (duplicate resource)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "DUPLICATE_RESOURCE",
                                    value = """
                                {
                                  "status": 409,
                                  "code": "DUPLICATE_RESOURCE",
                                  "message": "Product with name 'Phone' already exists",
                                  "path": "/product/1/update-product"
                                }"""
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "INTERNAL_ERROR",
                                    value = """
                                {
                                  "status": 500,
                                  "code": "INTERNAL_ERROR",
                                  "message": "Internal server error",
                                  "path": "/product/1/update-product"
                                }"""
                            )
                    )
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/update-product")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable("id") int productId,
            @RequestBody @Valid ProductUpdateDTO dto,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors())
            ErrorUtil.returnErrorsToClient(bindingResult);

        ProductResponseDTO responseDTO = productService.editProduct(dto, productId);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(
            summary = "Get all active (available) products",
            description = """
    Returns a list of all products that are currently marked as *available* (`availability = true`).

    Accessible to both **ROLE_USER** and **ROLE_ADMIN**.

    ### How to test in Swagger UI

    **200 OK (success):**
    1. `POST /auth/login` as either `ROLE_USER` or `ROLE_ADMIN` → copy the token.
    2. Click **Authorize** → `Bearer <token>`.
    3. `GET /product/all-active-products` → you'll get an array of active products.

    **401 UNAUTHORIZED:**
    - No token / malformed token / expired token → `401`.
    
    **423 ACCOUNT_LOCKED:**
    1. Login as a normal user → Authorize.
    2. `POST /auth/dev/_lock?username=<that_user>` → the account becomes locked.
    3. Call this endpoint → **423**.
    4. To restore → `POST /auth/dev/_unlock?username=<that_user>`.

    **Notes:**
    - Only products with `"availability": true` are returned.
    - The response is an array of `ProductResponseDTO` objects.
    - Endpoint is read-only and safe for both user and admin roles.
    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List of active (available) products",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ProductResponseDTO.class)),
                            examples = @ExampleObject(
                                    name = "OK",
                                    value = """
                                [
                                  {
                                    "id": 101,
                                    "productName": "Smartphone X",
                                    "productDescription": "Latest Android phone",
                                    "productCategory": "SMARTPHONES",
                                    "price": 799.99,
                                    "availability": true
                                  },
                                  {
                                    "id": 102,
                                    "productName": "Wireless Headphones",
                                    "productDescription": "Noise-cancelling Bluetooth headset",
                                    "productCategory": "ACCESSORIES",
                                    "price": 149.99,
                                    "availability": true
                                  }
                                ]
                                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized (missing or invalid token)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = {
                                    @ExampleObject(name = "UNAUTHORIZED", value = """
                                {
                                  "status": 401,
                                  "code": "UNAUTHORIZED",
                                  "message": "Full authentication is required to access this resource",
                                  "path": "/product/all-active-products"
                                }"""),
                                    @ExampleObject(name = "TOKEN_EXPIRED", value = """
                                {
                                  "status": 401,
                                  "code": "TOKEN_EXPIRED",
                                  "message": "The refresh token has expired.",
                                  "path": "/product/all-active-products"
                                }""")
                            }
                    )
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
                          "path": "/product/all-active-products"
                        }
                        """
                            ))),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "INTERNAL_ERROR",
                                    value = """
                                {
                                  "status": 500,
                                  "code": "INTERNAL_ERROR",
                                  "message": "Internal server error",
                                  "path": "/product/all-active-products"
                                }"""
                            )
                    )
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/all-active-products")
    public ResponseEntity<List<ProductResponseDTO>> findAvailableProducts() {
        return ResponseEntity.ok(productService.getAvailableProducts());
    }
}
