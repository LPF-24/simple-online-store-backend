package com.simple_online_store_backend.service;

import com.simple_online_store_backend.dto.PersonRequestDTO;
import com.simple_online_store_backend.dto.PersonResponseDTO;
import com.simple_online_store_backend.repository.PeopleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PeopleService {
    public final PeopleRepository peopleRepository;

    public PersonResponseDTO register(PersonRequestDTO personRequestDTO) {

    }
}
