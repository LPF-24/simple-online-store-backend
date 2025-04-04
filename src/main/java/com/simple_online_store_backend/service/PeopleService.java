package com.simple_online_store_backend.service;

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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final OrderRepository orderRepository;
    @Value("${admin.registration.code}")
    private String adminCodeFromYml;

    @Transactional
    public void register(PersonRequestDTO requestDTO) {
        Person person = personConverter.convertToPersonToRequest(requestDTO);
        if (requestDTO.getSpecialCode() != null && requestDTO.getSpecialCode().equals(adminCodeFromYml)) {
            person.setRole("ROLE_ADMIN");
        } else {
            person.setRole("ROLE_USER");
        }
        peopleRepository.saveAndFlush(person);
    }

    public List<PersonResponseDTO> getAllConsumers() {
        return peopleRepository.findAllByRole("ROLE_USER").stream().map(personConverter::convertToResponseDTO).toList();
    }

    @Transactional
    public void deactivateUserAccount(int userId) {
        Person person = peopleRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with this id wasn't found!"));

        // 1. Отменяем все PENDING/PROCESSING заказы пользователя
        List<Order> ordersToCancel = orderRepository.findByPerson(person).stream()
                .filter(order -> order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.PROCESSING)
                .toList();

        for (Order order : ordersToCancel) {
            order.setStatus(OrderStatus.CANCELLED);
        }

        // 2. Обновляем статус пользователя
        person.setIsDeleted(true);

        // 3. Сохраняем всё (благодаря @Transactional, изменения каскадно применятся)
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

    @PreAuthorize("isAuthenticated()")
    public PersonResponseDTO getCurrentUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();

        Person person = peopleRepository.findById(personDetails.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return personConverter.convertToResponseDTO(person);
    }
}
