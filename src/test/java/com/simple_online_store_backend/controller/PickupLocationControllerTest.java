package com.simple_online_store_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}

