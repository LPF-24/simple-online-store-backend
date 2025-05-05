package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.dto.address.AddressRequestDTO;
import com.simple_online_store_backend.dto.address.AddressResponseDTO;
import com.simple_online_store_backend.exception.ErrorUtil;
import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.AddressService;
import com.simple_online_store_backend.service.PeopleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Address", description = "Endpoints for work with address")
@RequestMapping("/address")
@RequiredArgsConstructor
@RestController
public class AddressController {
    private final AddressService addressService;
    private final PeopleService peopleService;

    @Operation(summary = "Add an address", description = "Adds an address for the user")
    @ApiResponse(responseCode = "200", description = "Successfully added")
    @ApiResponse(responseCode = "500", description = "Error inside method")
    @ApiResponse(responseCode = "403", description = "User is authenticated but not allowed to access this resource")
    @ApiResponse(responseCode = "400", description = "Request is invalid or missing required parameters")
    @ApiResponse(responseCode = "422", description = "Validation failed on request data")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/add-address")
    public ResponseEntity<AddressResponseDTO> addAddress(@RequestBody @Valid AddressRequestDTO dto,
                                                         BindingResult bindingResult) {
        if (bindingResult.hasErrors())
            ErrorUtil.returnErrorsToClient(bindingResult);

        int userId = getUserId();

        AddressResponseDTO address = addressService.addAddress(dto, userId);
        return ResponseEntity.ok(address);
    }

    @Operation(summary = "Update an address", description = "Updates an address for the user")
    @ApiResponse(responseCode = "200", description = "Successfully updated")
    @ApiResponse(responseCode = "500", description = "Error inside method")
    @ApiResponse(responseCode = "422", description = "Request is syntactically correct but semantically invalid")
    @ApiResponse(responseCode = "403", description = "User is authenticated but not allowed to access this resource")
    @ApiResponse(responseCode = "400", description = "Request is invalid or missing required parameters")
    @SecurityRequirement(name = "bearerAuth")
    @RequestMapping(value = "/update-address", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<AddressResponseDTO> updateAddress(@RequestBody @Valid AddressRequestDTO dto,
                                                            BindingResult bindingResult) {
        if (bindingResult.hasErrors())
            ErrorUtil.returnErrorsToClient(bindingResult);

        int userId = getUserId();
        int addressId = peopleService.getAddressIdByPersonId(userId);

        AddressResponseDTO updatedAddress = addressService.updateAddress(addressId, dto);
        return ResponseEntity.ok(updatedAddress);
    }

    private static int getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return ((PersonDetails) authentication.getPrincipal()).getId();
    }
}
