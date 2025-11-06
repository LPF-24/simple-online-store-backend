package com.simple_online_store_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simple_online_store_backend.dto.product.ProductRequestDTO;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.entity.Product;
import com.simple_online_store_backend.enums.ProductCategory;
import com.simple_online_store_backend.repository.PeopleRepository;
import com.simple_online_store_backend.repository.ProductRepository;
import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.ProductService;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.*;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTests {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired ProductRepository productRepository;

    @Autowired PeopleRepository peopleRepository;

    @MockitoSpyBean ProductService productService;

    @BeforeEach
    void setup() {
        productRepository.deleteAll();
        peopleRepository.deleteAll();
        Mockito.reset(productService);
        SecurityContextHolder.clearContext();
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

    private Person saveUser(String username, String email, String role) {
        Person p = new Person();
        p.setUserName(username);
        p.setEmail(email);
        p.setPassword("encoded");
        p.setRole(role);
        p.setDeleted(false);
        return peopleRepository.save(p);
    }

    private UsernamePasswordAuthenticationToken auth(Person p) {
        PersonDetails pd = new PersonDetails(p);
        return new UsernamePasswordAuthenticationToken(
                pd, null, java.util.List.of(new SimpleGrantedAuthority(p.getRole())));
    }

    private String body(String name, String desc, String price, boolean availability, ProductCategory category) throws Exception {
        ProductRequestDTO dto = new ProductRequestDTO();
        dto.setProductName(name);
        dto.setProductDescription(desc);
        dto.setPrice(new BigDecimal(price));
        dto.setAvailability(availability);
        dto.setProductCategory(category);
        return objectMapper.writeValueAsString(dto);
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

    @Nested
    class methodAddProduct {

        // ============ 200 OK: ADMIN создаёт товар ============
        @Test
        void add_admin_valid_returns200_andPersists() throws Exception {
            var admin = saveUser("admin", "admin@example.com", "ROLE_ADMIN");

            mvc.perform(post("/product/add-product")
                            .with(authentication(auth(admin)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Phone", "Android smartphone", "499.99", true, ProductCategory.SMARTPHONES))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.productName").value("Phone"))
                    .andExpect(jsonPath("$.productDescription").value("Android smartphone"))
                    .andExpect(jsonPath("$.productCategory").value("SMARTPHONES"))
                    .andExpect(jsonPath("$.price").value(closeTo(499.99, 0.01)))
                    .andExpect(jsonPath("$.availability").value(true));

            var all = productRepository.findAll();
            Assertions.assertEquals(1, all.size());
            Product saved = all.get(0);
            Assertions.assertEquals("Phone", saved.getProductName());
            Assertions.assertEquals("Android smartphone", saved.getProductDescription());
            Assertions.assertEquals(new BigDecimal("499.99"), saved.getPrice());
            Assertions.assertEquals(true, saved.getAvailability());
            Assertions.assertEquals(ProductCategory.SMARTPHONES, saved.getProductCategory());
        }

        // ============ 403 FORBIDDEN: USER не имеет прав ============
        @Test
        void add_user_forbidden_returns403() throws Exception {
            var user = saveUser("maria", "maria@example.com", "ROLE_USER");

            mvc.perform(post("/product/add-product")
                            .with(authentication(auth(user)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Phone", "Android smartphone", "499.99", true, ProductCategory.SMARTPHONES))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.code", anyOf(equalTo("ACCESS_DENIED"), equalTo("FORBIDDEN"))))
                    .andExpect(jsonPath("$.path").value("/product/add-product"));
        }

        // ============ 401 UNAUTHORIZED: без токена ============
        @Test
        void add_unauthorized_returns401() throws Exception {
            mvc.perform(post("/product/add-product")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Phone", "Android smartphone", "499.99", true, ProductCategory.SMARTPHONES))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.code", anyOf(
                            equalTo("MISSING_AUTH_HEADER"),
                            equalTo("UNAUTHORIZED"),
                            equalTo("INVALID_ACCESS_TOKEN"),
                            equalTo("TOKEN_EXPIRED")
                    )))
                    .andExpect(jsonPath("$.path").value("/product/add-product"));
        }

        // ============ 400 BAD_REQUEST: валидация DTO ============
        @Test
        void add_invalidBody_returns400() throws Exception {
            var admin = saveUser("admin", "admin@example.com", "ROLE_ADMIN");

            // Пустые поля / некорректные значения
            mvc.perform(post("/product/add-product")
                            .with(authentication(auth(admin)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("", "", "-1", true, ProductCategory.ACCESSORIES))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code", anyOf(equalTo("VALIDATION_ERROR"), equalTo("BAD_REQUEST"))))
                    .andExpect(jsonPath("$.path").value("/product/add-product"));
        }

        // ============ 500 INTERNAL_SERVER_ERROR: сервис упал ============
        @Test
        void add_serviceThrows_returns500() throws Exception {
            var admin = saveUser("root", "root@example.com", "ROLE_ADMIN");
            var token = auth(admin);

            // @PreAuthorize → перед стаббингом положим auth в SecurityContext
            SecurityContextHolder.getContext().setAuthentication(token);
            try {
                doThrow(new RuntimeException("DB down")).when(productService).addProduct(any(ProductRequestDTO.class));

                mvc.perform(post("/product/add-product")
                                .with(authentication(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("Phone", "Android smartphone", "499.99", true, ProductCategory.SMARTPHONES))
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.status").value(500))
                        .andExpect(jsonPath("$.code", anyOf(equalTo("INTERNAL_ERROR"), equalTo("SERVER_ERROR"))))
                        .andExpect(jsonPath("$.path").value("/product/add-product"));
            } finally {
                SecurityContextHolder.clearContext();
            }
        }

        // ============ 400: duplicate product name (validation or conflict) ============
        @Test
        void add_duplicateName_returns400_andDoesNotDuplicate() throws Exception {
            var admin = saveUser("admin", "admin@example.com", "ROLE_ADMIN");

            Product existing = new Product();
            existing.setProductName("Phone");
            existing.setProductDescription("Android smartphone");
            existing.setPrice(new java.math.BigDecimal("499.99"));
            existing.setAvailability(true);
            existing.setProductCategory(ProductCategory.SMARTPHONES);
            productRepository.save(existing);

            var result = mvc.perform(post("/product/add-product")
                            .with(authentication(auth(admin)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Phone", "Another desc", "599.99", true, ProductCategory.SMARTPHONES))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest()) // ожидаем 400 если стоит предчек в сервисе
                    .andExpect(jsonPath("$.status", anyOf(equalTo(400), equalTo(409))))
                    .andExpect(jsonPath("$.code", anyOf(
                            equalTo("VALIDATION_ERROR"),
                            equalTo("DUPLICATE_PRODUCT_NAME"),
                            equalTo("DATA_INTEGRITY_VIOLATION")
                    )))
                    .andExpect(jsonPath("$.path").value("/product/add-product"))
                    .andReturn();

            var all = productRepository.findAll();
            long phones = all.stream().filter(p -> "Phone".equals(p.getProductName())).count();
            Assertions.assertEquals(1, phones, "should not create duplicate product names");
        }
    }
}

