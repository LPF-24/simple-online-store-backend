package com.simple_online_store_backend.service;

import com.simple_online_store_backend.dto.person.PersonRequestDTO;
import com.simple_online_store_backend.dto.person.PersonResponseDTO;
import com.simple_online_store_backend.entity.Address;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.mapper.PersonConverter;
import com.simple_online_store_backend.repository.PeopleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PeopleService {
    private final PeopleRepository peopleRepository;
    private final PersonConverter personConverter;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(PersonRequestDTO personRequestDTO) {
        System.out.println("Method register started");
        System.out.println("DTO: " + personRequestDTO);
        Person person = personConverter.convertToPersonToRequest(personRequestDTO);
        person.setRole("ROLE_USER");
        System.out.println("Middle of the method register");
        peopleRepository.saveAndFlush(person);
        System.out.println("Method register ended");
    }

    public List<PersonResponseDTO> getAllConsumers() {
        return peopleRepository.findAllByRole("ROLE_USER").stream().map(personConverter::convertToResponseDTO).toList();
    }

    @Transactional
    public void deactivateUserAccount(int userId) {
        Person person = peopleRepository.findById(userId).orElseThrow(
                () -> new EntityNotFoundException("User with this id wasn't found!"));

        person.setIsDeleted(true);
        peopleRepository.save(person);
    }

    @Transactional
    public void restoreAccount(String username, String rawPassword) {
        Person person = peopleRepository.findByUserName(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!person.getIsDeleted()) {
            throw new IllegalStateException("Account is already active");
        }

        if (!passwordEncoder.matches(rawPassword, person.getPassword())) {
            throw new BadCredentialsException("Incorrect password");
        }

        person.setIsDeleted(false);
        peopleRepository.save(person);
    }

    public int getAddressIdByPersonId(int personId) {
        Person person = peopleRepository.findById(personId).orElseThrow(() ->
                new EntityNotFoundException("User with this id wasn't found!"));

        return Optional.ofNullable(person.getAddress())
                .map(Address::getId)
                .orElseThrow(() -> new EntityNotFoundException("The user has not yet specified their address."));
    }
}
