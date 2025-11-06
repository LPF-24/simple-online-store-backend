package com.simple_online_store_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simple_online_store_backend.dto.pickup_location.PickupLocationRequestDTO;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.entity.PickupLocation;
import com.simple_online_store_backend.repository.PeopleRepository;
import com.simple_online_store_backend.repository.PickupLocationRepository;
import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.PickupLocationService;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PickupLocationControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired PickupLocationRepository pickupLocationRepository;
    @Autowired PeopleRepository peopleRepository;

    @MockitoSpyBean PickupLocationService pickupLocationService;

    @BeforeEach
    void setup() {
        pickupLocationRepository.deleteAll();
        peopleRepository.deleteAll();
        Mockito.reset(pickupLocationService);
        SecurityContextHolder.clearContext();
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
        var pd = new PersonDetails(p);
        return new UsernamePasswordAuthenticationToken(
                pd, null, java.util.List.of(new SimpleGrantedAuthority(p.getRole())));
    }

    private PickupLocation saveLocation(
            String city, String street, String house, boolean active) {
        var loc = new PickupLocation();
        loc.setCity(city);
        loc.setStreet(street);
        loc.setHouseNumber(house);
        loc.setActive(active);
        return pickupLocationRepository.save(loc);
    }

    private String body(String city, String street, String house) throws Exception {
        var dto = new PickupLocationRequestDTO();
        dto.setCity(city);
        dto.setStreet(street);
        dto.setHouseNumber(house);
        return objectMapper.writeValueAsString(dto);
    }

    @Nested
    class methodGetAllPickupLocations {

        // ============ 200 OK: ADMIN видит все записи (active и inactive) ============
        @Test
        void getAll_admin_seesAll_returns200() throws Exception {
            var admin = saveUser("admin", "admin@example.com", "ROLE_ADMIN");

            var active = saveLocation("Berlin", "Main", "1A", true);
            var inactive = saveLocation("Munich", "Kaufingerstr.", "12", false);

            mvc.perform(get("/pickup/all-pickup-location")
                            .with(authentication(auth(admin)))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].id", hasItems(active.getId(), inactive.getId())))
                    .andExpect(jsonPath("$[?(@.id==" + inactive.getId() + ")].city").value(hasItem("Munich")))
                    .andExpect(jsonPath("$[?(@.id==" + active.getId() + ")].city").value(hasItem("Berlin")));
        }

        // ============ 200 OK: USER видит только активные записи ============
        @Test
        void getAll_user_seesOnlyActive_returns200() throws Exception {
            var user = saveUser("maria", "maria@example.com", "ROLE_USER");

            var active = saveLocation("Berlin", "Main", "1A", true);
            saveLocation("Munich", "Kaufingerstr.", "12", false);

            mvc.perform(get("/pickup/all-pickup-location")
                            .with(authentication(auth(user)))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(active.getId()))
                    .andExpect(jsonPath("$[0].city").value("Berlin"))
                    .andExpect(jsonPath("$[0].street").value("Main"))
                    .andExpect(jsonPath("$[0].houseNumber").value("1A"));
        }

        // ============ 200 OK: пустая БД → [] ============
        @Test
        void getAll_empty_returnsEmptyArray() throws Exception {
            var user = saveUser("ghost", "ghost@example.com", "ROLE_USER");

            mvc.perform(get("/pickup/all-pickup-location")
                            .with(authentication(auth(user)))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        // ============ 401 UNAUTHORIZED: без аутентификации ============
        @Test
        void getAll_unauthorized_returns401() throws Exception {
            mvc.perform(get("/pickup/all-pickup-location")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.code", anyOf(
                            equalTo("MISSING_AUTH_HEADER"),
                            equalTo("UNAUTHORIZED"),
                            equalTo("INVALID_ACCESS_TOKEN"),
                            equalTo("TOKEN_EXPIRED")
                    )))
                    .andExpect(jsonPath("$.path").value("/pickup/all-pickup-location"));
        }

        // ============ 500 INTERNAL_SERVER_ERROR: сервис упал ============
        @Test
        void getAll_serviceThrows_returns500() throws Exception {
            var user = saveUser("nick", "nick@example.com", "ROLE_USER");
            var token = auth(user);

            // Контроллер берёт роль из SecurityContext → положим туда токен
            SecurityContextHolder.getContext().setAuthentication(token);
            try {
                doThrow(new RuntimeException("DB down"))
                        .when(pickupLocationService).getAllPickupLocations(anyString());

                mvc.perform(get("/pickup/all-pickup-location")
                                .with(authentication(token))
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.status").value(500))
                        .andExpect(jsonPath("$.code", anyOf(equalTo("INTERNAL_ERROR"), equalTo("SERVER_ERROR"))))
                        .andExpect(jsonPath("$.path").value("/pickup/all-pickup-location"));
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }

    @Nested
    class methodAddPickupLocation {

        // ============ 200 OK: ADMIN создаёт новую точку самовывоза ============
        @Test
        void add_admin_valid_returns200_andPersists() throws Exception {
            var admin = saveUser("admin", "admin@example.com", "ROLE_ADMIN");

            mvc.perform(post("/pickup/add-pickup-location")
                            .with(authentication(auth(admin)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Berlin", "Main", "1A"))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.city").value("Berlin"))
                    .andExpect(jsonPath("$.street").value("Main"))
                    .andExpect(jsonPath("$.houseNumber").value("1A"));

            // убедимся, что запись реально появилась в БД
            var all = pickupLocationRepository.findAll();
            Assertions.assertEquals(1, all.size());
            PickupLocation saved = all.get(0);
            Assertions.assertEquals("Berlin", saved.getCity());
            Assertions.assertEquals("Main", saved.getStreet());
            Assertions.assertEquals("1A", saved.getHouseNumber());
        }

        // ============ 403 FORBIDDEN: USER не имеет прав (PreAuthorize в сервисе) ============
        @Test
        void add_user_forbidden_returns403() throws Exception {
            var user = saveUser("maria", "maria@example.com", "ROLE_USER");
            var token = auth(user);

            SecurityContextHolder.getContext().setAuthentication(token);
            try {
                mvc.perform(post("/pickup/add-pickup-location")
                                .with(authentication(auth(user)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("Berlin", "Main", "1A"))
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.status").value(403))
                        .andExpect(jsonPath("$.code", anyOf(equalTo("ACCESS_DENIED"), equalTo("FORBIDDEN"))))
                        .andExpect(jsonPath("$.path").value("/pickup/add-pickup-location"));
            } finally {
                SecurityContextHolder.clearContext();
            }
        }

        // ============ 401 UNAUTHORIZED: без токена ============
        @Test
        void add_unauthorized_returns401() throws Exception {
            mvc.perform(post("/pickup/add-pickup-location")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Berlin", "Main", "1A"))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.code", anyOf(
                            equalTo("MISSING_AUTH_HEADER"),
                            equalTo("UNAUTHORIZED"),
                            equalTo("INVALID_ACCESS_TOKEN"),
                            equalTo("TOKEN_EXPIRED")
                    )))
                    .andExpect(jsonPath("$.path").value("/pickup/add-pickup-location"));
        }

        // ============ 400 BAD_REQUEST: валидация DTO (city/street/houseNumber) ============
        @Test
        void add_invalidBody_returns400_withValidationErrors() throws Exception {
            var admin = saveUser("admin", "admin@example.com", "ROLE_ADMIN");

            // нарушим все три поля: пустые значения
            mvc.perform(post("/pickup/add-pickup-location")
                            .with(authentication(auth(admin)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("", "", ""))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code", anyOf(equalTo("VALIDATION_ERROR"), equalTo("BAD_REQUEST"))))
                    .andExpect(jsonPath("$.path").value("/pickup/add-pickup-location"));
        }

        // ============ 500 INTERNAL_SERVER_ERROR: сервис упал во время сохранения ============
        @Test
        void add_serviceThrows_returns500() throws Exception {
            var admin = saveUser("root", "root@example.com", "ROLE_ADMIN");
            var token = auth(admin);

            SecurityContextHolder.getContext().setAuthentication(token);
            try {
                doThrow(new RuntimeException("DB down"))
                        .when(pickupLocationService)
                        .addPickupLocation(any(PickupLocationRequestDTO.class));

                mvc.perform(post("/pickup/add-pickup-location")
                                .with(authentication(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("Berlin", "Main", "1A"))
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.status").value(500))
                        .andExpect(jsonPath("$.code", anyOf(equalTo("INTERNAL_ERROR"), equalTo("SERVER_ERROR"))))
                        .andExpect(jsonPath("$.path").value("/pickup/add-pickup-location"));
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }

    @Nested
    class methodClosePickupLocation {

        // ============ 200 OK: ADMIN закрывает существующую точку ==========
        @Test
        void close_admin_existing_returns200_andPersists() throws Exception {
            var admin = saveUser("admin", "admin@example.com", "ROLE_ADMIN");
            var location = saveLocation("Berlin", "Main", "1A", true);

            mvc.perform(patch("/pickup/" + location.getId() + "/close-pick-up-location")
                            .with(authentication(auth(admin)))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value("Pick-up location with id " + location.getId() + " closed."));

            // убедимся, что запись реально стала неактивной
            var updated = pickupLocationRepository.findById(location.getId()).orElseThrow();
            Assertions.assertFalse(updated.getActive(), "location must be inactive after close");
        }

        // ============ 403 FORBIDDEN: USER не имеет прав ==========
        @Test
        void close_user_forbidden_returns403_andDoesNotChangeEntity() throws Exception {
            var user = saveUser("maria", "maria@example.com", "ROLE_USER");
            var token = auth(user);
            var location = saveLocation("Berlin", "Main", "1A", true);

            SecurityContextHolder.getContext().setAuthentication(token);
            try {
                mvc.perform(patch("/pickup/" + location.getId() + "/close-pick-up-location")
                                .with(authentication(token))
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.status").value(403))
                        .andExpect(jsonPath("$.code", anyOf(equalTo("ACCESS_DENIED"), equalTo("FORBIDDEN"))))
                        .andExpect(jsonPath("$.path").value("/pickup/" + location.getId() + "/close-pick-up-location"));

                // состояние не должно меняться
                var same = pickupLocationRepository.findById(location.getId()).orElseThrow();
                Assertions.assertTrue(same.getActive(), "location must remain active for ROLE_USER");
            } finally {
                SecurityContextHolder.clearContext();
            }
        }

        // ============ 401 UNAUTHORIZED: без токена ==========
        @Test
        void close_unauthorized_returns401() throws Exception {
            var location = saveLocation("Berlin", "Main", "1A", true);

            mvc.perform(patch("/pickup/" + location.getId() + "/close-pick-up-location")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.code", anyOf(
                            equalTo("MISSING_AUTH_HEADER"),
                            equalTo("UNAUTHORIZED"),
                            equalTo("INVALID_ACCESS_TOKEN"),
                            equalTo("TOKEN_EXPIRED")
                    )))
                    .andExpect(jsonPath("$.path").value("/pickup/" + location.getId() + "/close-pick-up-location"));
        }

        // ============ 404 NOT FOUND: несуществующий id ==========
        @Test
        void close_admin_notFound_returns404() throws Exception {
            var admin = saveUser("admin", "admin@example.com", "ROLE_ADMIN");
            int missingId = 999_999;

            mvc.perform(patch("/pickup/" + missingId + "/close-pick-up-location")
                            .with(authentication(auth(admin)))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code").value("ENTITY_NOT_FOUND"))
                    .andExpect(jsonPath("$.message", containsString("doesn't exist")))
                    .andExpect(jsonPath("$.path").value("/pickup/" + missingId + "/close-pick-up-location"));
        }

        // ============ 500 INTERNAL_SERVER_ERROR: сервис упал ==========
        @Test
        void close_serviceThrows_returns500() throws Exception {
            var admin = saveUser("root", "root@example.com", "ROLE_ADMIN");
            var token = auth(admin);
            var location = saveLocation("Berlin", "Main", "1A", true);

            SecurityContextHolder.getContext().setAuthentication(token);
            try {
                doThrow(new RuntimeException("DB down"))
                        .when(pickupLocationService)
                        .closePickupLocation(anyInt());

                mvc.perform(patch("/pickup/" + location.getId() + "/close-pick-up-location")
                                .with(authentication(token))
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.status").value(500))
                        .andExpect(jsonPath("$.code", anyOf(equalTo("INTERNAL_ERROR"), equalTo("SERVER_ERROR"))))
                        .andExpect(jsonPath("$.path").value("/pickup/" + location.getId() + "/close-pick-up-location"));
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }
}

