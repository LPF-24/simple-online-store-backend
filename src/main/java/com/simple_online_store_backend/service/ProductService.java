package com.simple_online_store_backend.service;

import com.simple_online_store_backend.dto.product.ProductRequestDTO;
import com.simple_online_store_backend.dto.product.ProductResponseDTO;
import com.simple_online_store_backend.entity.Product;
import com.simple_online_store_backend.mapper.ProductMapper;
import com.simple_online_store_backend.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

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

    //этот метод getNullPropertyNames возвращает список имён полей объекта, значение которых равно null
    private String[] getNullPropertyNames(Object source) {
        try {
            return Arrays.stream(Introspector.getBeanInfo(source.getClass(), Object.class) //получаем информацию об объекте
                    .getPropertyDescriptors()) //получаем информацию о полях
                    .map(PropertyDescriptor::getName) //получаем имя поля
                    .filter(name -> { //проходим по именам
                        try {
                            return Objects.isNull(new PropertyDescriptor(name, source.getClass()).getReadMethod().invoke(source));
                        } catch (Exception e) {
                            return false; //в случае если при проверке нулевое ли поле возникла ошибка, возвращаем false
                        }
                    })
                    .toArray(String[]::new); //все ненулевые поля объединяем в массив
        } catch (IntrospectionException e) {
            throw new RuntimeException("error processing object properties", e); //если не удалось выполнить код в блоке try (возникла IntrospectionException), бросаем ошибку
        }

        /*
        new PropertyDescriptor(name, source.getClass())
        PropertyDescriptor — класс, который содержит метаинформацию о поле (геттеры и сеттеры).
        Мы создаем дескриптор для поля с именем name, взятого из source.getClass() — т.е. у текущего класса объекта.
        Например, если поле называется "price", создается PropertyDescriptor для price.

        .getReadMethod()
        Получает геттер для этого поля.
        Например, для поля "price" вернется метод getPrice().

        .invoke(source)
        Вызывает геттер у объекта source.
        Т.е. это аналог source.getPrice().
        Возвращает значение поля — может быть null или не null.

        Objects.isNull(...)
        Просто проверка: результат вызова геттера == null.*/
    }
}
