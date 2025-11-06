package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.dto.pickup_location.PickupLocationRequestDTO;
import com.simple_online_store_backend.dto.pickup_location.PickupLocationResponseDTO;
import com.simple_online_store_backend.exception.ErrorResponseDTO;
import com.simple_online_store_backend.exception.ErrorUtil;
import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.PickupLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Operation(
            summary = "Get all pickup locations (by role)",
            description = """
    Returns pickup locations depending on the caller's role.

    - `ROLE_ADMIN` → sees **all** locations (active and inactive).
    - `ROLE_USER`  → sees **only active** locations.

    ### How to test in Swagger UI

    **200 OK (success for USER):**
    1. `POST /auth/login` as a user with `ROLE_USER` → copy `token`.
    2. Click **Authorize** → `Bearer <token>`.
    3. `GET /pickup/all-pickup-location` → you'll get an array of **active** locations.

    **200 OK (success for ADMIN):**
    1. `POST /auth/login` as a user with `ROLE_ADMIN` → copy `token`.
    2. Click **Authorize** → `Bearer <token>`.
    3. `GET /pickup/all-pickup-location` → you'll get **all** locations (active + inactive).

    **401 UNAUTHORIZED:**
    - No token / malformed token / expired token → `401` (see response schema below).

    **Notes:**
    - Endpoint requires authentication but is **not** admin-only.
    - Response is a *list* of `PickupLocationResponseDTO`.
    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of pickup locations",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = PickupLocationResponseDTO.class)),
                            examples = {
                                    @ExampleObject(
                                            name = "USER (only active)",
                                            value = """
                                            [
                                              { "id": 1, "city": "Berlin",  "street": "Main",          "houseNumber": "1A", "active": true },
                                              { "id": 3, "city": "Hamburg", "street": "Speicherstadt", "houseNumber": "7",  "active": true }
                                            ]
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "ADMIN (all, including inactive)",
                                            value = """
                                            [
                                              { "id": 1, "city": "Berlin",  "street": "Main",          "houseNumber": "1A", "active": true },
                                              { "id": 2, "city": "Munich",  "street": "Kaufingerstr.", "houseNumber": "12", "active": false },
                                              { "id": 3, "city": "Hamburg", "street": "Speicherstadt", "houseNumber": "7",  "active": true }
                                            ]
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "Empty list",
                                            value = "[]"
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = {
                                    @ExampleObject(name = "MISSING_AUTH_HEADER", value = """
                            {
                              "status": 401,
                              "code": "MISSING_AUTH_HEADER",
                              "message": "Authentication header missing",
                              "path": "/pickup/all-pickup-location"
                            }"""),
                                    @ExampleObject(name = "INVALID_ACCESS_TOKEN", value = """
                            {
                              "status": 401,
                              "code": "INVALID_ACCESS_TOKEN",
                              "message": "Invalid access token",
                              "path": "/pickup/all-pickup-location"
                            }"""),
                                    @ExampleObject(name = "TOKEN_EXPIRED", value = """
                            {
                              "status": 401,
                              "code": "TOKEN_EXPIRED",
                              "message": "The access token has expired.",
                              "path": "/pickup/all-pickup-location"
                            }""")
                            }
                    )
            ),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "INTERNAL_ERROR", value = """
                    {
                      "status": 500,
                      "code": "INTERNAL_ERROR",
                      "message": "Internal server error",
                      "path": "/pickup/all-pickup-location"
                    }""")
                    )
            )
    })
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
