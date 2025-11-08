package com.simple_online_store_backend.unit.service;

import com.simple_online_store_backend.dto.person.PersonRequestDTO;
import com.simple_online_store_backend.dto.person.PersonResponseDTO;
import com.simple_online_store_backend.entity.Address;
import com.simple_online_store_backend.entity.Order;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.enums.OrderStatus;
import com.simple_online_store_backend.mapper.PersonConverter;
import com.simple_online_store_backend.repository.OrderRepository;
import com.simple_online_store_backend.repository.PeopleRepository;
import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.PeopleService;
import com.simple_online_store_backend.service.RefreshTokenService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PeopleServiceTests {

    @Mock PeopleRepository peopleRepository;
    @Mock PersonConverter personConverter;
    @Mock PasswordEncoder passwordEncoder;
    @Mock OrderRepository orderRepository;
    @Mock
    RefreshTokenService refreshTokenService;

    @Mock SecurityContext securityContext;
    @Mock Authentication authentication;

    @InjectMocks
    PeopleService peopleService;

    @BeforeEach
    void injectAdminCode() {
        ReflectionTestUtils.setField(peopleService, "adminCodeFromYml", "ADMIN-CODE");
    }

    @AfterEach
    void clearCtx() {
        SecurityContextHolder.clearContext();
    }

    // ---------- register

    @Test
    void register_setsAdminRole_whenSpecialCodeMatches() {
        PersonRequestDTO req = new PersonRequestDTO();
        req.setSpecialCode("ADMIN-CODE");

        Person personToSave = new Person();
        Person saved = new Person();
        saved.setId(1);
        saved.setRole("ROLE_ADMIN");

        when(personConverter.convertToPersonToRequest(req)).thenReturn(personToSave);
        when(peopleRepository.saveAndFlush(personToSave)).thenReturn(saved);

        PersonResponseDTO resp = new PersonResponseDTO();
        resp.setId(1);
        resp.setRole("ROLE_ADMIN");
        when(personConverter.convertToResponseDTO(saved)).thenReturn(resp);

        PersonResponseDTO result = peopleService.register(req);

        assertEquals("ROLE_ADMIN", result.getRole());
        assertEquals("ROLE_ADMIN", saved.getRole());
        verify(peopleRepository).saveAndFlush(personToSave);
    }

    @Test
    void register_setsUserRole_whenSpecialCodeMissingOrWrong() {
        PersonRequestDTO req = new PersonRequestDTO();
        req.setSpecialCode("WRONG");

        Person toSave = new Person();
        Person saved = new Person();
        saved.setId(2);
        saved.setRole("ROLE_USER");

        when(personConverter.convertToPersonToRequest(req)).thenReturn(toSave);
        when(peopleRepository.saveAndFlush(toSave)).thenReturn(saved);

        PersonResponseDTO resp = new PersonResponseDTO();
        resp.setId(2);
        resp.setRole("ROLE_USER");
        when(personConverter.convertToResponseDTO(saved)).thenReturn(resp);

        PersonResponseDTO result = peopleService.register(req);

        assertEquals("ROLE_USER", result.getRole());
    }

    // ---------- getAllConsumers

    @Test
    void getAllConsumers_returnsMappedDtos_forRoleUser() {
        Person u1 = new Person(); u1.setId(1); u1.setRole("ROLE_USER");
        Person u2 = new Person(); u2.setId(2); u2.setRole("ROLE_USER");

        when(peopleRepository.findAllByRole("ROLE_USER")).thenReturn(List.of(u1, u2));

        PersonResponseDTO r1 = new PersonResponseDTO(); r1.setId(1);
        PersonResponseDTO r2 = new PersonResponseDTO(); r2.setId(2);
        when(personConverter.convertToResponseDTO(u1)).thenReturn(r1);
        when(personConverter.convertToResponseDTO(u2)).thenReturn(r2);

        var list = peopleService.getAllConsumers();
        assertEquals(2, list.size());
        assertEquals(1, list.get(0).getId());
        assertEquals(2, list.get(1).getId());
    }

    // ---------- deactivateUserAccount

    @Test
    void deactivateUserAccount_cancelsPendingProcessing_setsDeleted_andSaves() {
        int userId = 10;
        Person person = new Person(); person.setId(userId); person.setDeleted(false);

        Order o1 = new Order(); o1.setStatus(OrderStatus.PENDING);
        Order o2 = new Order(); o2.setStatus(OrderStatus.PROCESSING);
        Order o3 = new Order(); o3.setStatus(OrderStatus.DELIVERED);

        when(peopleRepository.findById(userId)).thenReturn(Optional.of(person));
        when(orderRepository.findByPerson(person)).thenReturn(List.of(o1, o2, o3));

        peopleService.deactivateUserAccount(userId);

        assertEquals(OrderStatus.CANCELLED, o1.getStatus());
        assertEquals(OrderStatus.CANCELLED, o2.getStatus());
        assertEquals(OrderStatus.DELIVERED, o3.getStatus());
        assertTrue(person.getDeleted());
        verify(peopleRepository).save(person);
    }

    @Test
    void deactivateUserAccount_throwsWhenUserNotFound() {
        when(peopleRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> peopleService.deactivateUserAccount(99));
    }

    // ---------- setLocked

    @Test
    void setLocked_updatesDeletedFlag_andReturnsIt() {
        Person p = new Person(); p.setUserName("john"); p.setDeleted(false);
        when(peopleRepository.findByUserName("john")).thenReturn(Optional.of(p));

        boolean result = peopleService.setLocked("john", true);

        assertTrue(result);
        assertTrue(p.getDeleted());
        verify(peopleRepository).save(p);
    }

    @Test
    void setLocked_throwsWhenUserNotFound() {
        when(peopleRepository.findByUserName("absent")).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> peopleService.setLocked("absent", true));
    }

    // ---------- restoreAccount

    @Test
    void restoreAccount_unlocksWhenPasswordMatches() {
        Person p = new Person();
        p.setUserName("john");
        p.setDeleted(true);
        p.setPassword("ENC");

        when(peopleRepository.findByUserName("john")).thenReturn(Optional.of(p));
        when(passwordEncoder.matches("raw", "ENC")).thenReturn(true);

        peopleService.restoreAccount("john", "raw");

        assertFalse(p.getDeleted());
        verify(peopleRepository).save(p);
    }

    @Test
    void restoreAccount_throwsIfAlreadyActive() {
        Person p = new Person(); p.setUserName("john"); p.setDeleted(false);
        when(peopleRepository.findByUserName("john")).thenReturn(Optional.of(p));

        assertThrows(IllegalStateException.class, () -> peopleService.restoreAccount("john", "any"));
        verify(peopleRepository, never()).save(any());
    }

    @Test
    void restoreAccount_throwsIfBadPassword() {
        Person p = new Person(); p.setUserName("john"); p.setDeleted(true); p.setPassword("ENC");
        when(peopleRepository.findByUserName("john")).thenReturn(Optional.of(p));
        when(passwordEncoder.matches("wrong", "ENC")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> peopleService.restoreAccount("john", "wrong"));
        verify(peopleRepository, never()).save(any());
    }

    // ---------- getAddressIdByPersonId

    @Test
    void getAddressIdByPersonId_returnsAddressId() {
        Person p = new Person();
        Address a = new Address(); a.setId(7);
        p.setAddress(a);

        when(peopleRepository.findById(5)).thenReturn(Optional.of(p));

        int id = peopleService.getAddressIdByPersonId(5);
        assertEquals(7, id);
    }

    @Test
    void getAddressIdByPersonId_throwsWhenNoAddress() {
        Person p = new Person(); // address = null
        when(peopleRepository.findById(5)).thenReturn(Optional.of(p));

        assertThrows(EntityNotFoundException.class, () -> peopleService.getAddressIdByPersonId(5));
    }

    @Test
    void getAddressIdByPersonId_throwsWhenUserNotFound() {
        when(peopleRepository.findById(5)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> peopleService.getAddressIdByPersonId(5));
    }

    // ---------- getCurrentUserInfo

    @Test
    void getCurrentUserInfo_returnsResponseDTO_ofAuthenticatedUser() {
        Person domain = new Person(); domain.setId(1);
        PersonDetails details = new PersonDetails(domain);

        PersonResponseDTO dto = new PersonResponseDTO(); dto.setId(1);

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(details);
        when(peopleRepository.findById(1)).thenReturn(Optional.of(domain));
        when(personConverter.convertToResponseDTO(domain)).thenReturn(dto);

        PersonResponseDTO result = peopleService.getCurrentUserInfo();

        assertEquals(1, result.getId());
        verify(peopleRepository).findById(1);
        verify(personConverter).convertToResponseDTO(domain);
    }

    // ---------- promote / demote

    @Test
    void promotePerson_setsRoleAdmin_andSaves() {
        Person p = new Person(); p.setId(3); p.setRole("ROLE_USER");
        when(peopleRepository.findById(3)).thenReturn(Optional.of(p));

        peopleService.promotePerson(3);

        assertEquals("ROLE_ADMIN", p.getRole());
        verify(peopleRepository).save(p);
    }

    @Test
    void promotePerson_throwsWhenNotFound() {
        when(peopleRepository.findById(3)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> peopleService.promotePerson(3));
    }

    @Test
    void demoteToUserByUsername_setsRoleUser_andDeletesRefreshToken_whenWasAdmin() {
        Person p = new Person(); p.setUserName("john"); p.setRole("ROLE_ADMIN");
        when(peopleRepository.findByUserName("john")).thenReturn(Optional.of(p));

        peopleService.demoteToUserByUsername("john");

        assertEquals("ROLE_USER", p.getRole());
        verify(peopleRepository).save(p);
        verify(refreshTokenService).deleteRefreshToken("john");
    }

    @Test
    void demoteToUserByUsername_doesNothing_whenAlreadyUser() {
        Person p = new Person(); p.setUserName("john"); p.setRole("ROLE_USER");
        when(peopleRepository.findByUserName("john")).thenReturn(Optional.of(p));

        peopleService.demoteToUserByUsername("john");

        verify(peopleRepository, never()).save(any());
        verify(refreshTokenService, never()).deleteRefreshToken(anyString());
    }

    @Test
    void demoteToUserByUsername_throwsWhenNotFound() {
        when(peopleRepository.findByUserName("absent")).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> peopleService.demoteToUserByUsername("absent"));
    }
}

