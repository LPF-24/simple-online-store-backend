package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.dto.product.ProductRequestDTO;
import com.simple_online_store_backend.dto.product.ProductResponseDTO;
import com.simple_online_store_backend.exception.ErrorUtil;
import com.simple_online_store_backend.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Products", description = "Managing products")
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @Operation(summary = "Get all products", description = "Returns a list of all products (including unavailable)")
    @ApiResponse(responseCode = "200", description = "List of products successfully retrieved")
    @GetMapping()
    public ResponseEntity<List<ProductResponseDTO>> allProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @Operation(summary = "Add new product", description = "Allows admin to add a new product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product successfully added"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Only admin can add products")
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
