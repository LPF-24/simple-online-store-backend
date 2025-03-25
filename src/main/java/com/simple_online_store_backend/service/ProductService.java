package com.simple_online_store_backend.service;

import com.simple_online_store_backend.dto.product.ProductRequestDTO;
import com.simple_online_store_backend.dto.product.ProductResponseDTO;
import com.simple_online_store_backend.entity.Product;
import com.simple_online_store_backend.mapper.ProductMapper;
import com.simple_online_store_backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ProductResponseDTO addProduct(ProductRequestDTO dto) {
        Product productToAdd = productMapper.mapRequestDTOToProduct(dto);
        productRepository.save(productToAdd);
        return productMapper.mapProductToResponseDTO(productToAdd);
    }
}
