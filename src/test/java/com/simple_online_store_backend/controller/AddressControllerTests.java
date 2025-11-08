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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

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

    @MockitoSpyBean
    AddressController addressController;

    @MockitoSpyBean
    AddressService addressService;

    @BeforeEach
    void clean() {
        peopleRepository.findAll().forEach(p -> p.setAddress(null));
        peopleRepository.saveAll(peopleRepository.findAll());
        addressRepository.deleteAll();
        peopleRepository.deleteAll();
        Mockito.reset(addressService, addressController);
    }

    private String validJson(String city, String street, String houseNumber,
                             String apartment, String postalCode, String deliveryType,
                             String housingType ) throws Exception {
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

    @Nested
    class methodAddAddressTests {
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

            Person updated = peopleRepository.findById(userId).orElseThrow();
            assertEquals(existing.getId(), updated.getAddress().getId(), "Must reuse the existing address entity");
            assertEquals(beforeCount, addressRepository.count(), "No duplicate addresses must be created");
        }

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

        @Test
        void addAddress_malformedJson_returns400() throws Exception {
            int userId = createUser("nick", "nick@example.com", "ROLE_USER");
            doReturn(userId).when(addressController).getUserId();

            String malformed = "{ \"city\": \"Berlin\", ";

            mvc.perform(post("/address/add-address")
                            .with(user("nick").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformed))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code",
                            anyOf(equalTo("MESSAGE_NOT_READABLE"), equalTo("BAD_REQUEST"))))
                    .andExpect(jsonPath("$.path", containsString("/address/add-address")));
        }

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

    @Nested
    class updateAddressTests {
        private int createUser(String username, String email, String role) {
            Person p = new Person();
            p.setUserName(username);
            p.setEmail(email);
            p.setPassword("encoded");
            p.setRole(role);
            p.setDeleted(false);
            return peopleRepository.save(p).getId();
        }

        private String json(AddressRequestDTO dto) throws Exception {
            return objectMapper.writeValueAsString(dto);
        }

        private AddressRequestDTO validDto(String city, String street, String house, String apt, String zip) {
            var dto = new AddressRequestDTO();
            dto.setCity(city);
            dto.setStreet(street);
            dto.setHouseNumber(house);
            dto.setApartment(apt);
            dto.setPostalCode(zip);
            dto.setDeliveryType(com.simple_online_store_backend.enums.DeliveryType.POSTAL);
            dto.setHousingType(com.simple_online_store_backend.enums.HousingType.APARTMENT);
            return dto;
        }

        @Test
        void updateAddress_success_updatesAllFields() throws Exception {
            int userId = createUser("masha", "masha@example.com", "ROLE_USER");
            Address old = createAddress("Berlin", "Main Street", "12A", "45", "10115");
            var u = peopleRepository.findById(userId).orElseThrow();
            u.setAddress(old);
            peopleRepository.save(u);

            doReturn(userId).when(addressController).getUserId();

            var dto = validDto("Munich", "Ludwigstrasse", "10", "7", "80333");

            mvc.perform(patch("/address/update-address")
                            .with(user("masha").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(dto)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.city").value("Munich"))
                    .andExpect(jsonPath("$.street").value("Ludwigstrasse"))
                    .andExpect(jsonPath("$.houseNumber").value("10"))
                    .andExpect(jsonPath("$.apartment").value("7"))
                    .andExpect(jsonPath("$.postalCode").value("80333"));

            Address updated = addressRepository.findById(old.getId()).orElseThrow();
            assertEquals("Munich", updated.getCity());
            assertEquals("Ludwigstrasse", updated.getStreet());
            assertEquals("10", updated.getHouseNumber());
            assertEquals("7", updated.getApartment());
            assertEquals("80333", updated.getPostalCode());
        }

        @Test
        void updateAddress_success_partialUpdatePostalCodeOnly() throws Exception {
            int userId = createUser("john", "john@example.com", "ROLE_USER");
            Address old = createAddress("Berlin", "Main Street", "12A", "45", "10115");
            var u = peopleRepository.findById(userId).orElseThrow();
            u.setAddress(old);
            peopleRepository.save(u);

            doReturn(userId).when(addressController).getUserId();

            var dto = validDto("Berlin", "Main Street", "12A", "45", "10117");

            mvc.perform(patch("/address/update-address")
                            .with(user("john").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.postalCode").value("10117"))
                    .andExpect(jsonPath("$.city").value("Berlin"));

            Address after = addressRepository.findById(old.getId()).orElseThrow();
            assertEquals("10117", after.getPostalCode(), "postalCode must be updated");
            assertEquals("Berlin", after.getCity(), "city must stay the same");
        }

        @Test
        void updateAddress_validationErrors_returns400() throws Exception {
            int userId = createUser("kate", "kate@example.com", "ROLE_USER");
            Address old = createAddress("Berlin", "Main Street", "12A", "45", "10115");
            var u = peopleRepository.findById(userId).orElseThrow();
            u.setAddress(old);
            peopleRepository.save(u);

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

            mvc.perform(patch("/address/update-address")
                            .with(user("kate").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bad))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
                    .andExpect(jsonPath("$.path", containsString("/address/update-address")))
                    .andExpect(jsonPath("$.message", not(blankOrNullString())));
        }

        @Test
        void updateAddress_malformedJson_returns400() throws Exception {
            int userId = createUser("nick", "nick@example.com", "ROLE_USER");
            Address old = createAddress("Berlin", "Main Street", "12A", "45", "10115");
            var u = peopleRepository.findById(userId).orElseThrow();
            u.setAddress(old);
            peopleRepository.save(u);

            doReturn(userId).when(addressController).getUserId();

            String malformed = "{ \"city\": \"Berlin\", ";

            mvc.perform(patch("/address/update-address")
                            .with(user("nick").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformed))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code",
                            anyOf(equalTo("MESSAGE_NOT_READABLE"), equalTo("BAD_REQUEST"))))
                    .andExpect(jsonPath("$.path", containsString("/address/update-address")));
        }

        @Test
        void updateAddress_userHasNoAddress_returns404() throws Exception {
            int userId = createUser("noaddr", "noaddr@example.com", "ROLE_USER");
            doReturn(userId).when(addressController).getUserId();

            var dto = validDto("Berlin", "Main Street", "12A", "45", "10115");

            mvc.perform(patch("/address/update-address")
                            .with(user("noaddr").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(dto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code", anyOf(equalTo("ENTITY_NOT_FOUND"), equalTo("NOT_FOUND"))))
                    .andExpect(jsonPath("$.message", containsString("has not yet specified")))
                    .andExpect(jsonPath("$.path", containsString("/address/update-address")));
        }

        @Test
        void updateAddress_invalidToken_returns401() throws Exception {
            var dto = validDto("Berlin", "Main Street", "12A", "45", "10115");

            mvc.perform(patch("/address/update-address")
                            .header("Authorization", "Bearer invalid.token.signature")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(dto)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
        }

        @Test
        void updateAddress_forbidden_returns403() throws Exception {
            int userId = createUser("admin", "admin@example.com", "ROLE_ADMIN");
            doReturn(userId).when(addressController).getUserId();

            var dto = validDto("Berlin", "Main Street", "12A", "45", "10115");

            mvc.perform(patch("/address/update-address")
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(dto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void updateAddress_serviceThrows_returns500() throws Exception {
            int userId = createUser("peter", "peter@example.com", "ROLE_USER");
            Address old = createAddress("Berlin", "Main Street", "12A", "45", "10115");
            var u = peopleRepository.findById(userId).orElseThrow();
            u.setAddress(old);
            peopleRepository.save(u);

            doReturn(userId).when(addressController).getUserId();

            var dto = validDto("Cologne", "Domstrasse", "1", "1", "50667");

            doThrow(new RuntimeException("DB down"))
                    .when(addressService).updateAddress(anyInt(), Mockito.any(AddressRequestDTO.class));

            mvc.perform(patch("/address/update-address")
                            .with(user("peter").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(dto)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.code", anyOf(equalTo("INTERNAL_ERROR"), equalTo("SERVER_ERROR"))))
                    .andExpect(jsonPath("$.path", containsString("/address/update-address")));
        }
    }

    @Nested
    class methodDeleteAddressTests {
        @Test
        void deleteAddress_success_singleOwner_removesRow() throws Exception {
            int userId = createUser("user1", "user1@example.com", "ROLE_USER");
            Address addr = createAddress("Berlin", "Main Street", "12A", "45", "10115");

            Person u = peopleRepository.findById(userId).orElseThrow();
            u.setAddress(addr);
            peopleRepository.save(u);

            long beforeAddrCount = addressRepository.count();
            doReturn(userId).when(addressController).getUserId();

            mvc.perform(delete("/address/delete-address")
                            .with(user("user1").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message", containsString("deleted")));

            Person updated = peopleRepository.findById(userId).orElseThrow();
            assertNull(updated.getAddress(), "Person.address must be null after deletion");
            assertEquals(beforeAddrCount - 1, addressRepository.count(), "Address row must be removed from DB");
            assertFalse(addressRepository.findById(addr.getId()).isPresent(), "Address must not exist anymore");
        }

        @Test
        void deleteAddress_success_sharedAddress_detachOnly() throws Exception {
            int userA = createUser("userA", "a@example.com", "ROLE_USER");
            int userB = createUser("userB", "b@example.com", "ROLE_USER");
            Address addr = createAddress("Munich", "Ludwigstrasse", "10", "7", "80333");

            Person a = peopleRepository.findById(userA).orElseThrow();
            Person b = peopleRepository.findById(userB).orElseThrow();
            a.setAddress(addr);
            b.setAddress(addr);
            peopleRepository.save(a);
            peopleRepository.save(b);

            long beforeAddrCount = addressRepository.count();
            doReturn(userA).when(addressController).getUserId();

            mvc.perform(delete("/address/delete-address")
                            .with(user("userA").roles("USER")))
                    .andExpect(status().isOk());

            Person a2 = peopleRepository.findById(userA).orElseThrow();
            Person b2 = peopleRepository.findById(userB).orElseThrow();
            assertNull(a2.getAddress(), "userA must be detached from address");
            assertNotNull(b2.getAddress(), "userB must still be attached");
            assertEquals(beforeAddrCount, addressRepository.count(), "Shared address must not be deleted");
            assertTrue(addressRepository.findById(addr.getId()).isPresent(), "Address must still exist");
        }

        @Test
        void deleteAddress_userHasNoAddress_returns404() throws Exception {
            int userId = createUser("noaddr", "noaddr@example.com", "ROLE_USER");
            doReturn(userId).when(addressController).getUserId();

            mvc.perform(delete("/address/delete-address")
                            .with(user("noaddr").roles("USER")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code", anyOf(equalTo("ENTITY_NOT_FOUND"), equalTo("NOT_FOUND"))))
                    .andExpect(jsonPath("$.message", anyOf(
                            containsString("not yet specified"),
                            containsString("no address")
                    )))
                    .andExpect(jsonPath("$.path", containsString("/address/delete-address")));
        }

        @Test
        void deleteAddress_unauthorized_returns401() throws Exception {
            mvc.perform(delete("/address/delete-address"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", anyOf(
                            equalTo("MISSING_AUTH_HEADER"),
                            equalTo("UNAUTHORIZED"),
                            equalTo("INVALID_ACCESS_TOKEN"),
                            equalTo("TOKEN_EXPIRED")
                    )));
        }

        @Test
        void deleteAddress_forbidden_returns403() throws Exception {
            int adminId = createUser("admin", "admin@example.com", "ROLE_ADMIN");
            doReturn(adminId).when(addressController).getUserId();

            mvc.perform(delete("/address/delete-address")
                            .with(user("admin").roles("ADMIN")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.code", anyOf(equalTo("ACCESS_DENIED"), equalTo("FORBIDDEN"))));
        }

        @Test
        void deleteAddress_serviceThrows_returns500() throws Exception {
            int userId = createUser("userX", "ux@example.com", "ROLE_USER");
            Address addr = createAddress("Cologne", "Domstrasse", "1", "1", "50667");
            Person u = peopleRepository.findById(userId).orElseThrow();
            u.setAddress(addr);
            peopleRepository.save(u);

            doReturn(userId).when(addressController).getUserId();

            doThrow(new RuntimeException("DB down"))
                    .when(addressService).deleteAddress(anyInt());

            mvc.perform(delete("/address/delete-address")
                            .with(user("userX").roles("USER")))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.code", anyOf(equalTo("INTERNAL_ERROR"), equalTo("SERVER_ERROR"))))
                    .andExpect(jsonPath("$.path", containsString("/address/delete-address")));
        }
    }

    private Address createAddress(String city, String street, String house, String apt, String zip) {
        Address a = new Address();
        a.setCity(city);
        a.setStreet(street);
        a.setHouseNumber(house);
        a.setApartment(apt);
        a.setPostalCode(zip);
        return addressRepository.save(a);
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
}

