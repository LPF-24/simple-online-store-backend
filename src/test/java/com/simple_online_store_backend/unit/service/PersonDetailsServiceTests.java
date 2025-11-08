package com.simple_online_store_backend.unit.service;

import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.repository.PeopleRepository;
import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.PersonDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonDetailsServiceTests {

    @Mock
    PeopleRepository peopleRepository;

    @InjectMocks
    PersonDetailsService personDetailsService;

    @Test
    void loadUserByUsername_returnsPersonDetails_whenUserExists() {
        // Arrange
        Person p = new Person();
        p.setId(1);
        p.setUserName("john");
        p.setPassword("ENC");
        p.setRole("ROLE_USER");

        when(peopleRepository.findByUserName("john")).thenReturn(Optional.of(p));

        // Act
        UserDetails details = personDetailsService.loadUserByUsername("john");

        // Assert
        assertNotNull(details);
        assertTrue(details instanceof PersonDetails);
        assertEquals("john", details.getUsername());
        assertEquals("ENC", details.getPassword());
        // assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));

        verify(peopleRepository).findByUserName("john");
        verifyNoMoreInteractions(peopleRepository);
    }

    @Test
    void loadUserByUsername_throwsUsernameNotFound_whenUserMissing() {
        // Arrange
        when(peopleRepository.findByUserName("absent")).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(UsernameNotFoundException.class,
                () -> personDetailsService.loadUserByUsername("absent"));

        verify(peopleRepository).findByUserName("absent");
        verifyNoMoreInteractions(peopleRepository);
    }
}

