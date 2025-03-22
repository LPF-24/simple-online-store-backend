package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.dto.address.AddressRequestDTO;
import com.simple_online_store_backend.dto.address.AddressResponseDTO;
import com.simple_online_store_backend.dto.address.AddressUpdateDTO;
import com.simple_online_store_backend.exception.ErrorUtil;
import com.simple_online_store_backend.security.JWTUtil;
import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.AddressService;
import com.simple_online_store_backend.service.PeopleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/address")
@RequiredArgsConstructor
@RestController
public class AddressController {
    private final AddressService addressService;
    private final PeopleService peopleService;

    @PostMapping("/add-address")
    public ResponseEntity<AddressResponseDTO> addAddress(@RequestBody @Valid AddressRequestDTO dto,
                                                         BindingResult bindingResult) {
        if (bindingResult.hasErrors())
            ErrorUtil.returnErrorsToClient(bindingResult);

        AddressResponseDTO address = addressService.addAddress(dto);
        return ResponseEntity.ok(address);
    }

    @PatchMapping("/update-address")
    public ResponseEntity<AddressResponseDTO> updateAddress(@RequestBody @Valid AddressUpdateDTO dto,
                                                            BindingResult bindingResult) {
        if (bindingResult.hasErrors())
            ErrorUtil.returnErrorsToClient(bindingResult);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = ((PersonDetails) authentication.getPrincipal()).getId();
        Integer addressId = peopleService.getAddressIdByPersonId(userId);

        AddressResponseDTO updatedAddress = addressService.updateAddress(addressId, dto);
        return ResponseEntity.ok(updatedAddress);
    }
}
