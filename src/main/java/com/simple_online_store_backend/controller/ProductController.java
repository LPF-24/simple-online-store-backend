package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.dto.product.ProductRequestDTO;
import com.simple_online_store_backend.dto.product.ProductResponseDTO;
import com.simple_online_store_backend.exception.ErrorUtil;
import com.simple_online_store_backend.service.ProductService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping()
    public ResponseEntity<List<ProductResponseDTO>> allProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/add-product")
    public ResponseEntity<ProductResponseDTO> addProduct(@RequestBody @Valid ProductRequestDTO dto,
                                                         BindingResult bindingResult) {
        if (bindingResult.hasErrors())
            ErrorUtil.returnErrorsToClient(bindingResult);

        ProductResponseDTO responseDTO = productService.addProduct(dto);
        return ResponseEntity.ok(responseDTO);
    }

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

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/all-active-products")
    public ResponseEntity<List<ProductResponseDTO>> findAvailableProducts() {
        List<ProductResponseDTO> activeProducts = productService.getAvailableProducts();
        return ResponseEntity.ok(activeProducts);
    }
}
