package com.simple_online_store_backend.service;

import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.repository.PeopleRepository;
import com.simple_online_store_backend.security.PersonDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PersonDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {
    private final PeopleRepository peopleRepository;

    @Autowired
    public PersonDetailsService(PeopleRepository peopleRepository) {
        this.peopleRepository = peopleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Person user = peopleRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username doesn't found!"));
        return new PersonDetails(user);
    }
}
