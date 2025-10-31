package com.simple_online_store_backend.service;

import com.simple_online_store_backend.dto.product.ProductRequestDTO;
import com.simple_online_store_backend.dto.product.ProductResponseDTO;
import com.simple_online_store_backend.entity.Product;
import com.simple_online_store_backend.mapper.ProductMapper;
import com.simple_online_store_backend.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    public List<ProductResponseDTO> getAllProducts() {
        return productRepository.findAll().stream().map(productMapper::mapProductToResponseDTO).toList();
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ProductResponseDTO addProduct(ProductRequestDTO dto) {
        Product productToAdd = productMapper.mapRequestDTOToProduct(dto);
        productRepository.save(productToAdd);
        return productMapper.mapProductToResponseDTO(productToAdd);
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ProductResponseDTO editProduct(ProductRequestDTO dto, Integer productId) {
        Product productToUpdate = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product with id " + productId + " wasn't found"));

        BeanUtils.copyProperties(dto, productToUpdate, getNullPropertyNames(dto));
        productRepository.save(productToUpdate);
        return productMapper.mapProductToResponseDTO(productToUpdate);
    }

    @PreAuthorize("isAuthenticated()")
    public List<ProductResponseDTO> getAvailableProducts() {
        List<Product> available = productRepository.findAllByAvailabilityTrue();
        return available.stream().map(productMapper::mapProductToResponseDTO).toList();
    }

    // Returns names of properties that are null in the given object
    private String[] getNullPropertyNames(Object source) {
        try {
            return Arrays.stream(Introspector.getBeanInfo(source.getClass(), Object.class)
                    .getPropertyDescriptors()) // Get property metadata
                    .map(PropertyDescriptor::getName) // Extract property names
                    .filter(name -> { /// Filter out non-null properties
                        try {
                            return Objects.isNull(new PropertyDescriptor(name, source.getClass()).getReadMethod().invoke(source));
                        } catch (Exception e) {
                            return false; // If error occurs, skip the property
                        }
                    })
                    .toArray(String[]::new);
        } catch (IntrospectionException e) {
            throw new RuntimeException("error processing object properties", e);
        }

        /*
         Explanation:
         - PropertyDescriptor(name, clazz) retrieves metadata about a property, including getters/setters.
         - .getReadMethod() returns the getter (e.g., getPrice()).
         - .invoke(source) calls the getter on the given object, like source.getPrice().
         - Objects.isNull(...) checks whether the returned value is null.
         */
    }
}
