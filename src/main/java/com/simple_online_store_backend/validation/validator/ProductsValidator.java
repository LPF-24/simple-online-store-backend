package com.simple_online_store_backend.validation.validator;

import com.simple_online_store_backend.dto.order.OrderRequestDTO;
import com.simple_online_store_backend.entity.Product;
import com.simple_online_store_backend.repository.ProductRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/*public class ProductsValidator implements ConstraintValidator<ValidProductsList, OrderRequestDTO> {
    private final ProductRepository productRepository;

    @Autowired
    public ProductsValidator(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public boolean isValid(OrderRequestDTO dto, ConstraintValidatorContext context) {
        List<Product> products = dto.getProducts();
        if (products == null || products.isEmpty())
            return false;

        return products.stream().allMatch(product -> {
            Optional<Product> productFromDb = productRepository.findById(product.getId());
            return productFromDb.isPresent() && Boolean.TRUE.equals(productFromDb.get().getAvailability());
        });
    }
}*/
