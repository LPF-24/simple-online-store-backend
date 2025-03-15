package com.simple_online_store_backend.service;

import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.repository.CustomerRepository;
import com.simple_online_store_backend.security.CustomerDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomerDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {
    private final CustomerRepository customerRepository;

    @Autowired
    public CustomerDetailsService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Person user = customerRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username doesn't found!"));
        return new CustomerDetails(user);
    }
}
