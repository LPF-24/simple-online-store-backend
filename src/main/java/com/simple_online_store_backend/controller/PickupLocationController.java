package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.dto.pickup_location.PickupLocationRequestDTO;
import com.simple_online_store_backend.dto.pickup_location.PickupLocationResponseDTO;
import com.simple_online_store_backend.exception.ErrorUtil;
import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.PickupLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Pickup Locations", description = "Manage pick-up locations")
@RestController
@RequestMapping("/pickup")
public class PickupLocationController {
    private final PickupLocationService service;

    public PickupLocationController(PickupLocationService service) {
        this.service = service;
    }

    @Operation(summary = "Get all pick-up locations", description = "Returns list of pick-up points for users/admins")
    @ApiResponse(responseCode = "200", description = "List successfully returned")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/all-pickup-location")
    public ResponseEntity<List<PickupLocationResponseDTO>> getAllPickupLocations() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String role = ((PersonDetails) authentication.getPrincipal())
                .getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_USER");
        return ResponseEntity.ok(service.getAllPickupLocations(role));
    }

    @Operation(summary = "Add new pick-up location", description = "Allows admin to add a pick-up point")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pick-up location successfully added"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Only admin can add pick-up locations")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/add-pickup-location")
    public ResponseEntity<PickupLocationResponseDTO> addPickupLocation(@RequestBody @Valid PickupLocationRequestDTO dto,
                                                                       BindingResult bindingResult) {
        if (bindingResult.hasErrors())
            ErrorUtil.returnErrorsToClient(bindingResult);

        return ResponseEntity.ok(service.addPickupLocation(dto));
    }

    @Operation(summary = "Close pick-up location", description = "Deactivates pick-up location by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pick-up location closed"),
            @ApiResponse(responseCode = "403", description = "Only admin can close pick-up locations"),
            @ApiResponse(responseCode = "404", description = "Location not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @RequestMapping(value = "/{id}/close-pick-up-location", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<HttpStatus> closePickupLocation(@PathVariable("id") int id) {
        service.closePickupLocation(id);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @Operation(summary = "Open pick-up location", description = "Reactivates pick-up location by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pick-up location opened"),
            @ApiResponse(responseCode = "403", description = "Only admin can open pick-up locations"),
            @ApiResponse(responseCode = "404", description = "Location not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @RequestMapping(value = "/{id}/open-pick-up-location", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<HttpStatus> openPickupLocation(@PathVariable("id") int id) {
        service.openPickupLocation(id);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @Operation(summary = "Update pick-up location", description = "Allows admin to update pick-up location by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pick-up location updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Only admin can update pick-up locations"),
            @ApiResponse(responseCode = "404", description = "Location not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @RequestMapping(value = "/{id}/update-pick-up-location", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<PickupLocationResponseDTO> updatePickupLocation(@RequestBody @Valid PickupLocationRequestDTO dto,
                                                                          @PathVariable("id") int id,
                                                                          BindingResult bindingResult) {
        if (bindingResult.hasErrors())
            ErrorUtil.returnErrorsToClient(bindingResult);

        return ResponseEntity.ok(service.updatePickupLocation(dto, id));
    }
}
