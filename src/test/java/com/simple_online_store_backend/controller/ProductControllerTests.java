package com.simple_online_store_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simple_online_store_backend.entity.Product;
import com.simple_online_store_backend.enums.ProductCategory;
import com.simple_online_store_backend.repository.ProductRepository;
import com.simple_online_store_backend.service.ProductService;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTests {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired ProductRepository productRepository;

    @MockitoSpyBean ProductService productService;

    @BeforeEach
    void setup() {
        productRepository.deleteAll();
        Mockito.reset(productService);
    }

    private Product saveProduct(String name, String desc, BigDecimal price, Boolean availability, ProductCategory category) {
        Product p = new Product();
        p.setProductName(name);
        p.setProductDescription(desc);
        p.setPrice(price);
        p.setAvailability(availability);
        p.setProductCategory(category);
        return productRepository.save(p);
    }

    @Nested
    class methodAllProducts {

        // ============ 200 OK: список продуктов возвращается без аутентификации (permitAll) ============
        @Test
        void all_noAuth_returns200_withItems() throws Exception {
            var p1 = saveProduct("Phone", "Android phone", new BigDecimal("499.99"), true, ProductCategory.SMARTPHONES);
            var p2 = saveProduct("Case", "Protective case", new BigDecimal("19.99"), false, ProductCategory.ACCESSORIES);

            mvc.perform(get("/product").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].id", hasItems(p1.getId(), p2.getId())))
                    .andExpect(jsonPath("$[?(@.id==" + p1.getId() + ")].productName").value(hasItem("Phone")))
                    .andExpect(jsonPath("$[?(@.id==" + p2.getId() + ")].availability").value(hasItem(false)))
                    .andExpect(jsonPath("$[?(@.id==" + p1.getId() + ")].price").value(hasItem(closeTo(499.99, 0.01))));
        }

        // ============ 200 OK: пустой список ============
        @Test
        void all_empty_returnsEmptyArray() throws Exception {
            mvc.perform(get("/product").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        // ============ 200 OK: корректный content-type ============
        @Test
        void all_returnsJsonContentType() throws Exception {
            saveProduct("Phone", "Android phone", new BigDecimal("499.99"), true, ProductCategory.SMARTPHONES);

            mvc.perform(get("/product").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", containsString("application/json")));
        }

        // ============ 500 INTERNAL_SERVER_ERROR: сервис падает ============
        @Test
        void all_serviceThrows_returns500() throws Exception {
            doThrow(new RuntimeException("DB down")).when(productService).getAllProducts();

            mvc.perform(get("/product").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.code", anyOf(equalTo("INTERNAL_ERROR"), equalTo("SERVER_ERROR"))))
                    .andExpect(jsonPath("$.path").value("/product"));
        }
    }
}

