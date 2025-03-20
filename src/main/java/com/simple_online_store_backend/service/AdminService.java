package com.simple_online_store_backend.service;

import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.repository.PeopleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize(value = "ROLE_ADMIN")
@RequiredArgsConstructor
public class AdminService {
    private final PeopleRepository peopleRepository;

}
