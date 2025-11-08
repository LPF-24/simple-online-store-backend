package com.simple_online_store_backend.unit.service;

import com.simple_online_store_backend.dto.product.ProductRequestDTO;
import com.simple_online_store_backend.dto.product.ProductResponseDTO;
import com.simple_online_store_backend.dto.product.ProductUpdateDTO;
import com.simple_online_store_backend.entity.Product;
import com.simple_online_store_backend.exception.DuplicateResourceException;
import com.simple_online_store_backend.exception.ValidationException;
import com.simple_online_store_backend.mapper.ProductMapper;
import com.simple_online_store_backend.repository.ProductRepository;
import com.simple_online_store_backend.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTests {

    @Mock ProductRepository productRepository;
    @Mock ProductMapper productMapper;

    @InjectMocks ProductService productService;

    // ---------- getAllProducts

    @Test
    void getAllProducts_mapsList() {
        Product p1 = product(1, "A", true, "10.00");
        Product p2 = product(2, "B", false, "20.00");

        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        ProductResponseDTO r1 = resp(1, "A", "10.00", true);
        ProductResponseDTO r2 = resp(2, "B", "20.00", false);
        when(productMapper.mapProductToResponseDTO(p1)).thenReturn(r1);
        when(productMapper.mapProductToResponseDTO(p2)).thenReturn(r2);

        var out = productService.getAllProducts();

        assertEquals(2, out.size());
        assertEquals("A", out.get(0).getProductName());
        assertEquals("B", out.get(1).getProductName());
        verify(productRepository).findAll();
        verify(productMapper).mapProductToResponseDTO(p1);
        verify(productMapper).mapProductToResponseDTO(p2);
    }

    // ---------- addProduct

    @Test
    void addProduct_success_savesAndMaps() {
        ProductRequestDTO dto = new ProductRequestDTO();
        dto.setProductName("NewName");
        dto.setProductDescription("Nice thing");
        dto.setPrice(new BigDecimal("9.99"));
        dto.setAvailability(true);

        when(productRepository.existsByProductNameIgnoreCase("NewName")).thenReturn(false);

        Product toSave = new Product();
        toSave.setProductName("NewName");
        when(productMapper.mapRequestDTOToProduct(dto)).thenReturn(toSave);

        ProductResponseDTO resp = new ProductResponseDTO();
        resp.setProductName("NewName");
        when(productMapper.mapProductToResponseDTO(toSave)).thenReturn(resp);

        ProductResponseDTO out = productService.addProduct(dto);

        assertEquals("NewName", out.getProductName());
        verify(productRepository).save(toSave);
    }

    @Test
    void addProduct_throws_whenDuplicateName() {
        ProductRequestDTO dto = new ProductRequestDTO();
        dto.setProductName("Dup");

        when(productRepository.existsByProductNameIgnoreCase("Dup")).thenReturn(true);

        assertThrows(ValidationException.class, () -> productService.addProduct(dto));
        verify(productRepository, never()).save(any());
    }

    // ---------- editProduct

    @Test
    void editProduct_success_partialUpdate_andTrimName() {
        Integer id = 10;
        Product existing = product(id, "OldName", true, "5.00");
        when(productRepository.findById(id)).thenReturn(Optional.of(existing));

        ProductUpdateDTO patch = new ProductUpdateDTO();
        patch.setProductName("  NewName  ");
        patch.setPrice(new BigDecimal("7.50"));
        patch.setAvailability(false);

        when(productRepository.existsByProductNameIgnoreCaseAndIdNot("NewName", id)).thenReturn(false);

        Product saved = product(id, "NewName", false, "7.50");
        when(productRepository.save(existing)).thenReturn(saved);

        ProductResponseDTO mapped = resp(id, "NewName", "7.50", false);
        when(productMapper.mapProductToResponseDTO(saved)).thenReturn(mapped);

        ProductResponseDTO out = productService.editProduct(patch, id);

        assertEquals("NewName", out.getProductName());
        assertEquals(new BigDecimal("7.50"), existing.getPrice());
        assertFalse(existing.getAvailability());
        verify(productRepository).save(existing);
    }

    @Test
    void editProduct_throws_whenProductNotFound() {
        when(productRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> productService.editProduct(new ProductUpdateDTO(), 999));
    }

    @Test
    void editProduct_throws_whenNameBlankAfterTrim() {
        Integer id = 5;
        when(productRepository.findById(id)).thenReturn(Optional.of(product(id, "X", true, "1.00")));

        ProductUpdateDTO patch = new ProductUpdateDTO();
        patch.setProductName("   ");

        assertThrows(ValidationException.class, () -> productService.editProduct(patch, id));
        verify(productRepository, never()).save(any());
    }

    @Test
    void editProduct_throws_whenDuplicateNameForAnotherProduct() {
        Integer id = 7;
        when(productRepository.findById(id)).thenReturn(Optional.of(product(id, "Old", true, "1.00")));

        ProductUpdateDTO patch = new ProductUpdateDTO();
        patch.setProductName("New");

        when(productRepository.existsByProductNameIgnoreCaseAndIdNot("New", id)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> productService.editProduct(patch, id));
        verify(productRepository, never()).save(any());
    }

    @Test
    void editProduct_keepsOldName_whenNameNotProvidedButUpdatesOtherFields() {
        Integer id = 12;
        Product existing = product(id, "KeepMe", true, "10.00");
        when(productRepository.findById(id)).thenReturn(Optional.of(existing));

        ProductUpdateDTO patch = new ProductUpdateDTO();
        patch.setPrice(new BigDecimal("12.34"));

        Product saved = product(id, "KeepMe", true, "12.34");
        when(productRepository.save(existing)).thenReturn(saved);

        ProductResponseDTO mapped = resp(id, "KeepMe", "12.34", true);
        when(productMapper.mapProductToResponseDTO(saved)).thenReturn(mapped);

        ProductResponseDTO out = productService.editProduct(patch, id);

        assertEquals("KeepMe", out.getProductName());
        assertEquals(new BigDecimal("12.34"), existing.getPrice());
        verify(productRepository, never())
                .existsByProductNameIgnoreCaseAndIdNot(anyString(), anyInt());
        verify(productRepository).save(existing);
    }

    // ---------- getAvailableProducts

    @Test
    void getAvailableProducts_mapsOnlyAvailable() {
        Product p = product(1, "Only", true, "3.30");
        when(productRepository.findAllByAvailabilityTrue()).thenReturn(List.of(p));

        ProductResponseDTO r = resp(1, "Only", "3.30", true);
        when(productMapper.mapProductToResponseDTO(p)).thenReturn(r);

        var out = productService.getAvailableProducts();

        assertEquals(1, out.size());
        assertTrue(out.get(0).getAvailability());
        verify(productRepository).findAllByAvailabilityTrue();
    }

    // ---------- helpers

    private Product product(Integer id, String name, boolean available, String price) {
        Product p = new Product();
        p.setId(id);
        p.setProductName(name);
        p.setAvailability(available);
        p.setPrice(new BigDecimal(price));
        return p;
    }

    private ProductResponseDTO resp(Integer id, String name, String price, boolean available) {
        ProductResponseDTO r = new ProductResponseDTO();
        r.setId(id);
        r.setProductName(name);
        r.setPrice(new BigDecimal(price));
        r.setAvailability(available);
        return r;
    }
}

