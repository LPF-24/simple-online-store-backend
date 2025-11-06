package com.simple_online_store_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.simple_online_store_backend.dto.product.ProductRequestDTO;
import com.simple_online_store_backend.dto.product.ProductUpdateDTO;
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

import static org.mockito.ArgumentMatchers.anyInt;
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

    private String body(String name, String desc, String price, Boolean availability, ProductCategory category) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        if (name != null)        root.put("productName", name);
        if (desc != null)        root.put("productDescription", desc);
        if (price != null)       root.put("price", new java.math.BigDecimal(price)); // добавляем только если не null
        if (availability != null)root.put("availability", availability);
        if (category != null)    root.put("productCategory", category.name());
        return objectMapper.writeValueAsString(root);
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

    @Nested
    class methodUpdateProduct {

        // ============ 200 OK: ADMIN частично обновляет существующий продукт (меняем только name + price) ============
        @Test
        void update_admin_partial_returns200_andPersists() throws Exception {
            var admin = saveUser("admin", "admin@example.com", "ROLE_ADMIN");
            var product = saveProduct("Phone", "Android smartphone", BigDecimal.valueOf(499.99), true, ProductCategory.SMARTPHONES);

            mvc.perform(patch("/product/" + product.getId() + "/update-product")
                            .with(authentication(auth(admin)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Phone X", null, "549.99", true, null))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(product.getId()))
                    .andExpect(jsonPath("$.productName").value("Phone X"))
                    .andExpect(jsonPath("$.price").value(closeTo(549.99, 0.01)))
                    // unchanged fields
                    .andExpect(jsonPath("$.productDescription").value("Android smartphone"))
                    .andExpect(jsonPath("$.availability").value(true))
                    .andExpect(jsonPath("$.productCategory").value("SMARTPHONES"));

            var updated = productRepository.findById(product.getId()).orElseThrow();
            Assertions.assertEquals("Phone X", updated.getProductName());
            Assertions.assertEquals(new BigDecimal("549.99"), updated.getPrice());
            Assertions.assertEquals("Android smartphone", updated.getProductDescription());
            Assertions.assertEquals(true, updated.getAvailability());
            Assertions.assertEquals(ProductCategory.SMARTPHONES, updated.getProductCategory());
        }

        // ============ 200 OK: ADMIN полное обновление всеми полями тоже работает ============
        @Test
        void update_admin_full_returns200_andPersists() throws Exception {
            var admin = saveUser("admin", "admin@example.com", "ROLE_ADMIN");
            var product = saveProduct("Phone", "Android smartphone", BigDecimal.valueOf(499.99), true, ProductCategory.SMARTPHONES);

            mvc.perform(patch("/product/" + product.getId() + "/update-product")
                            .with(authentication(auth(admin)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Case", "Protective case", "19.99", false, ProductCategory.ACCESSORIES))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productName").value("Case"))
                    .andExpect(jsonPath("$.productDescription").value("Protective case"))
                    .andExpect(jsonPath("$.price").value(closeTo(19.99, 0.01)))
                    .andExpect(jsonPath("$.availability").value(false))
                    .andExpect(jsonPath("$.productCategory").value("ACCESSORIES"));

            var updated = productRepository.findById(product.getId()).orElseThrow();
            Assertions.assertEquals("Case", updated.getProductName());
            Assertions.assertEquals("Protective case", updated.getProductDescription());
            Assertions.assertEquals(new BigDecimal("19.99"), updated.getPrice());
            Assertions.assertEquals(false, updated.getAvailability());
            Assertions.assertEquals(ProductCategory.ACCESSORIES, updated.getProductCategory());
        }

        // ============ 400 BAD_REQUEST: некорректные значения (price < 0, короткое имя) ============
        @Test
        void update_invalidBody_returns400() throws Exception {
            var admin = saveUser("admin", "admin@example.com", "ROLE_ADMIN");
            var product = saveProduct("Phone", "Android smartphone", BigDecimal.valueOf(499.99), true, ProductCategory.SMARTPHONES);

            mvc.perform(patch("/product/" + product.getId() + "/update-product")
                            .with(authentication(auth(admin)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("", "short", "-1", true, null))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code", anyOf(equalTo("VALIDATION_ERROR"), equalTo("BAD_REQUEST"))))
                    .andExpect(jsonPath("$.path").value("/product/" + product.getId() + "/update-product"));
        }

        // ============ 403 FORBIDDEN: USER не имеет прав ============
        @Test
        void update_user_forbidden_returns403_andNotChanged() throws Exception {
            var user = saveUser("maria", "maria@example.com", "ROLE_USER");
            var token = auth(user);
            var product = saveProduct("Phone", "Android smartphone", BigDecimal.valueOf(499.99), true, ProductCategory.SMARTPHONES);

            SecurityContextHolder.getContext().setAuthentication(token);
            try {
                mvc.perform(patch("/product/" + product.getId() + "/update-product")
                                .with(authentication(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("Phone Z", null, null, true, null))
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.status").value(403))
                        .andExpect(jsonPath("$.code", anyOf(equalTo("ACCESS_DENIED"), equalTo("FORBIDDEN"))))
                        .andExpect(jsonPath("$.path").value("/product/" + product.getId() + "/update-product"));

                var same = productRepository.findById(product.getId()).orElseThrow();
                Assertions.assertEquals("Phone", same.getProductName());
            } finally {
                SecurityContextHolder.clearContext();
            }
        }

        // ============ 401 UNAUTHORIZED: без токена ============
        @Test
        void update_unauthorized_returns401() throws Exception {
            var product = saveProduct("Phone", "Android smartphone", BigDecimal.valueOf(499.99), true, ProductCategory.SMARTPHONES);

            mvc.perform(patch("/product/" + product.getId() + "/update-product")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Phone X", null, null, true, null))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.path").value("/product/" + product.getId() + "/update-product"));
        }

        // ============ 404 NOT FOUND: несуществующий id ============
        @Test
        void update_notFound_returns404() throws Exception {
            var admin = saveUser("admin", "admin@example.com", "ROLE_ADMIN");
            int missingId = 999_999;

            mvc.perform(patch("/product/" + missingId + "/update-product")
                            .with(authentication(auth(admin)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Phone X", null, null, true, null))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code").value("ENTITY_NOT_FOUND"))
                    .andExpect(jsonPath("$.message", containsString("wasn't found")))
                    .andExpect(jsonPath("$.path").value("/product/" + missingId + "/update-product"));
        }

        @Test
        void update_duplicateName_returns409() throws Exception {
            var admin = saveUser("admin", "admin@example.com", "ROLE_ADMIN");
            var p1 = saveProduct("Phone", "Android smartphone", BigDecimal.valueOf(499.99), true, ProductCategory.SMARTPHONES);
            var p2 = saveProduct("Case", "Protective case", BigDecimal.valueOf(19.99), true, ProductCategory.ACCESSORIES);

            mvc.perform(patch("/product/" + p2.getId() + "/update-product")
                            .with(authentication(auth(admin)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Phone", null, null, true, null))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", equalTo(409)))
                    .andExpect(jsonPath("$.code", equalTo("DUPLICATE_RESOURCE")))
                    .andExpect(jsonPath("$.path").value("/product/" + p2.getId() + "/update-product"));
        }

        // ============ 500 INTERNAL_SERVER_ERROR: эмуляция падения сервиса ============
        @Test
        void update_serviceThrows_returns500() throws Exception {
            var admin = saveUser("root", "root@example.com", "ROLE_ADMIN");
            var token = auth(admin);
            var product = saveProduct("Phone", "Android smartphone", BigDecimal.valueOf(499.99), true, ProductCategory.SMARTPHONES);

            SecurityContextHolder.getContext().setAuthentication(token);
            try {
                doThrow(new RuntimeException("DB down"))
                        .when(productService)
                        .editProduct(any(ProductUpdateDTO.class), anyInt());

                mvc.perform(patch("/product/" + product.getId() + "/update-product")
                                .with(authentication(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("Phone X", null, null, true, null))
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.status").value(500))
                        .andExpect(jsonPath("$.code", anyOf(equalTo("INTERNAL_ERROR"), equalTo("SERVER_ERROR"))))
                        .andExpect(jsonPath("$.path").value("/product/" + product.getId() + "/update-product"));
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }
}

