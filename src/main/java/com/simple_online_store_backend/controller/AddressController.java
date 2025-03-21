package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/address")
@RequiredArgsConstructor
public class AddressController {
    public final AddressService addressService;

    /*@PostMapping("/add-address")
    public ResponseEntity<>*/
}
