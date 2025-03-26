package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.dto.pickup_location.PickupLocationRequestDTO;
import com.simple_online_store_backend.dto.pickup_location.PickupLocationResponseDTO;
import com.simple_online_store_backend.exception.ErrorUtil;
import com.simple_online_store_backend.service.PickupLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pickup")
@RequiredArgsConstructor
public class PickupLocationController {
    private final PickupLocationService service;

    @PostMapping("/add-pickup-location")
    public ResponseEntity<PickupLocationResponseDTO> addPickupLocation(@RequestBody @Valid PickupLocationRequestDTO dto,
                                                                       BindingResult bindingResult) {
        if (bindingResult.hasErrors())
            ErrorUtil.returnErrorsToClient(bindingResult);

        PickupLocationResponseDTO response = service.addPickupLocation(dto);
        return ResponseEntity.ok(response);
    }
}
