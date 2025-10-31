package com.simple_online_store_backend.mapper;

import com.simple_online_store_backend.dto.product.ProductRequestDTO;
import com.simple_online_store_backend.dto.product.ProductResponseDTO;
import com.simple_online_store_backend.entity.Product;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    private final ModelMapper modelMapper;

    public ProductMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public Product mapRequestDTOToProduct(ProductRequestDTO dto) {
        return modelMapper.map(dto, Product.class);
    }

    public ProductResponseDTO mapProductToResponseDTO(Product product) {
        return modelMapper.map(product, ProductResponseDTO.class);
    }
}
