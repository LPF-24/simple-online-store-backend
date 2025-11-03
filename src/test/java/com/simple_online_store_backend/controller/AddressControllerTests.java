package com.simple_online_store_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simple_online_store_backend.dto.address.AddressRequestDTO;
import com.simple_online_store_backend.entity.Address;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.repository.AddressRepository;
import com.simple_online_store_backend.repository.PeopleRepository;
import com.simple_online_store_backend.service.AddressService;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AddressControllerTests {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired PeopleRepository peopleRepository;
    @Autowired AddressRepository addressRepository;

    // Спай контроллера — чтобы подменить getUserId()
    @MockitoSpyBean
    AddressController addressController;

    @MockitoSpyBean
    AddressService addressService;

    @BeforeEach
    void clean() {
        // чистим в правильном порядке (сначала адрес у юзеров, потом таблицы)
        peopleRepository.findAll().forEach(p -> p.setAddress(null));
        peopleRepository.saveAll(peopleRepository.findAll());
        addressRepository.deleteAll();
        peopleRepository.deleteAll();
        Mockito.reset(addressService, addressController);
    }

    private int createUser(String username, String email, String role) {
        Person p = new Person();
        p.setUserName(username);
        p.setEmail(email);
        p.setPassword("encoded");
        p.setRole(role);
        p.setDeleted(false);
        return peopleRepository.save(p).getId();
    }

    private String validJson(String city, String street, String houseNumber,
                             String apartment, String postalCode, String deliveryType,
                             String housingType /* может быть null */) throws Exception {
        AddressRequestDTO dto = new AddressRequestDTO();
        dto.setCity(city);
        dto.setStreet(street);
        dto.setHouseNumber(houseNumber);
        dto.setApartment(apartment);
        dto.setPostalCode(postalCode);
        dto.setDeliveryType(com.simple_online_store_backend.enums.DeliveryType.valueOf(deliveryType));
        if (housingType != null) {
            dto.setHousingType(com.simple_online_store_backend.enums.HousingType.valueOf(housingType));
        }
        return objectMapper.writeValueAsString(dto);
    }

    // === 1) Успех: адреса нет — создаём и привязываем к пользователю ===
    @Test
    void addAddress_success_createsNew() throws Exception {
        int userId = createUser("maria12", "maria12@gmail.com", "ROLE_USER");
        doReturn(userId).when(addressController).getUserId();

        String body = validJson(
                "Berlin", "Main Street", "12A", "45", "10115",
                "POSTAL", "APARTMENT"
        );

        mvc.perform(post("/address/add-address")
                        .with(user("maria12").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.city").value("Berlin"))
                .andExpect(jsonPath("$.street").value("Main Street"))
                .andExpect(jsonPath("$.houseNumber").value("12A"))
                .andExpect(jsonPath("$.apartment").value("45"))
                .andExpect(jsonPath("$.postalCode").value("10115"));

        Person updated = peopleRepository.findById(userId).orElseThrow();
        assertNotNull(updated.getAddress(), "User must be linked to the newly created address");
        Optional<Address> inDb = addressRepository.findById(updated.getAddress().getId());
        assertTrue(inDb.isPresent(), "Address entity must be persisted");
    }

    // === 2) Успех: адрес уже существует — переиспользуем без дубликатов ===
    @Test
    void addAddress_success_reusesExisting() throws Exception {
        int userId = createUser("john", "john@example.com", "ROLE_USER");
        doReturn(userId).when(addressController).getUserId();

        Address existing = new Address();
        existing.setCity("Munich");
        existing.setStreet("Ludwigstrasse");
        existing.setHouseNumber("10");
        existing.setApartment("7");
        existing.setPostalCode("80333");
        addressRepository.save(existing);

        long beforeCount = addressRepository.count();

        String body = validJson(
                "Munich", "Ludwigstrasse", "10", "7", "80333",
                "POSTAL", "APARTMENT"
        );

        mvc.perform(post("/address/add-address")
                        .with(user("john").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Munich"))
                .andExpect(jsonPath("$.street").value("Ludwigstrasse"))
                .andExpect(jsonPath("$.houseNumber").value("10"))
                .andExpect(jsonPath("$.apartment").value("7"))
                .andExpect(jsonPath("$.postalCode").value("80333"));

        // пользователь должен ссылаться на тот же адрес; количество записей не увеличилось
        Person updated = peopleRepository.findById(userId).orElseThrow();
        assertEquals(existing.getId(), updated.getAddress().getId(), "Must reuse the existing address entity");
        assertEquals(beforeCount, addressRepository.count(), "No duplicate addresses must be created");
    }

    // === 3) Валидации DTO: неверный формат/пустые поля → 400 VALIDATION_ERROR ===
    @Test
    void addAddress_validationErrors_returns400() throws Exception {
        int userId = createUser("kate", "kate@example.com", "ROLE_USER");
        doReturn(userId).when(addressController).getUserId();

        String bad = """
                    {
                      "city":"berlin",
                      "street":"",
                      "houseNumber":"???",
                      "apartment": null,
                      "postalCode":"not-zip",
                      "deliveryType":"POSTAL"
                    }
                    """;

        mvc.perform(post("/address/add-address")
                        .with(user("kate").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code", anyOf(equalTo("VALIDATION_ERROR"), equalTo("BAD_REQUEST"))))
                .andExpect(jsonPath("$.path", containsString("/address/add-address")))
                .andExpect(jsonPath("$.message", not(blankOrNullString())));
    }

    // === 5) Битый JSON → 400 MESSAGE_NOT_READABLE ===
    @Test
    void addAddress_malformedJson_returns400() throws Exception {
        int userId = createUser("nick", "nick@example.com", "ROLE_USER");
        doReturn(userId).when(addressController).getUserId();

        String malformed = "{ \"city\": \"Berlin\", "; // обрываем

        mvc.perform(post("/address/add-address")       // <-- путь, как в SecurityConfig
                        .with(user("nick").roles("USER"))      // <-- аутентификация с ролью
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformed))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code",
                        anyOf(equalTo("MESSAGE_NOT_READABLE"), equalTo("BAD_REQUEST"))))
                .andExpect(jsonPath("$.path", containsString("/address/add-address")));
    }

    // === 6) Внутренняя ошибка сервиса → 500 INTERNAL_ERROR ===
    @Test
    void addAddress_serviceThrows_returns500() throws Exception {
        int userId = createUser("peter", "peter@example.com", "ROLE_USER");
        doReturn(userId).when(addressController).getUserId();

        String body = validJson(
                "Cologne", "Domstrasse", "1", "1", "50667",
                "POSTAL", "APARTMENT"
        );

        doThrow(new RuntimeException("DB is down"))
                .when(addressService).addAddress(Mockito.any(AddressRequestDTO.class), anyInt());

        mvc.perform(post("/address/add-address")
                        .with(user("peter").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code", anyOf(equalTo("INTERNAL_ERROR"), equalTo("SERVER_ERROR"))))
                .andExpect(jsonPath("$.path", containsString("/address/add-address")));
    }
}

