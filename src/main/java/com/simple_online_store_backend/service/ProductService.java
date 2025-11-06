package com.simple_online_store_backend.service;

import com.simple_online_store_backend.dto.product.ProductRequestDTO;
import com.simple_online_store_backend.dto.product.ProductResponseDTO;
import com.simple_online_store_backend.dto.product.ProductUpdateDTO;
import com.simple_online_store_backend.entity.Product;
import com.simple_online_store_backend.exception.DuplicateResourceException;
import com.simple_online_store_backend.exception.ValidationException;
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
        if (productRepository.existsByProductNameIgnoreCase(dto.getProductName())) {
            throw new ValidationException("Product with name '" + dto.getProductName() + "' already exists");
        }
        Product productToAdd = productMapper.mapRequestDTOToProduct(dto);
        productRepository.save(productToAdd);
        return productMapper.mapProductToResponseDTO(productToAdd);
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ProductResponseDTO editProduct(ProductUpdateDTO dto, Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product with id " + productId + " wasn't found"));

        String newName = null;
        if (dto.getProductName() != null) {
            newName = dto.getProductName().trim();
            if (newName.isEmpty()) {
                throw new ValidationException("Product name must not be blank");
            }
            boolean duplicate = productRepository.existsByProductNameIgnoreCaseAndIdNot(newName, productId);
            if (duplicate) {
                throw new DuplicateResourceException("Product with name '" + newName + "' already exists");
            }
        }

        String[] ignore = merge(getNullPropertyNames(dto), new String[]{"productName"});
        BeanUtils.copyProperties(dto, product, ignore);

        if (newName != null) {
            product.setProductName(newName);
        }

        Product saved = productRepository.save(product);
        return productMapper.mapProductToResponseDTO(saved);
    }

    private static String[] merge(String[] a, String[] b) {
        String[] r = new String[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
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
