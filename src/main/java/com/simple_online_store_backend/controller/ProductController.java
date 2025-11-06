package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.dto.product.ProductRequestDTO;
import com.simple_online_store_backend.dto.product.ProductResponseDTO;
import com.simple_online_store_backend.exception.ErrorResponseDTO;
import com.simple_online_store_backend.exception.ErrorUtil;
import com.simple_online_store_backend.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "Update product", description = "Allows admin to update product by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product successfully updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Only admin can update products"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @RequestMapping(value = "/{id}/update-product", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<ProductResponseDTO> updateProduct(@PathVariable("id") int productId,
                                                            @RequestBody @Valid ProductRequestDTO dto,
                                                            BindingResult bindingResult) {
        if (bindingResult.hasErrors())
            ErrorUtil.returnErrorsToClient(bindingResult);

        ProductResponseDTO responseDTO = productService.editProduct(dto, productId);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Get available products", description = "Returns only available (active) products")
    @ApiResponse(responseCode = "200", description = "List of available products successfully retrieved")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/all-active-products")
    public ResponseEntity<List<ProductResponseDTO>> findAvailableProducts() {
        return ResponseEntity.ok(productService.getAvailableProducts());
    }
}
