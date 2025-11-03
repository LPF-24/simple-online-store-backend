package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.dto.address.AddressRequestDTO;
import com.simple_online_store_backend.dto.address.AddressResponseDTO;
import com.simple_online_store_backend.exception.ErrorResponseDTO;
import com.simple_online_store_backend.exception.ErrorUtil;
import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.AddressService;
import com.simple_online_store_backend.service.PeopleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Address", description = "Endpoints for work with address")
@RequestMapping("/address")
@RestController
public class AddressController {
    private final AddressService addressService;
    private final PeopleService peopleService;

    public AddressController(AddressService addressService, PeopleService peopleService) {
        this.addressService = addressService;
        this.peopleService = peopleService;
    }

    /*@Operation(summary = "Add an address", description = "Adds an address for the user")
    @ApiResponse(responseCode = "200", description = "Successfully added")
    @ApiResponse(responseCode = "500", description = "Error inside method")
    @ApiResponse(responseCode = "403", description = "User is authenticated but not allowed to access this resource")
    @ApiResponse(responseCode = "400", description = "Request is invalid or missing required parameters")
    @ApiResponse(responseCode = "422", description = "Validation failed on request data")
    @SecurityRequirement(name = "bearerAuth")*/
    @Operation(
            summary = "Add or reuse user's address",
            description = """
                            Adds a new address for the authenticated user, or reuses an existing identical address.  
                            Returns the stored address object.
                    
                            ### How to test in Swagger UI
                    
                            **200 OK (success):**
                            1. Call `/auth/login` with credentials of a user having `ROLE_USER` (e.g., `"user@example.com"`).  
                               Copy the `token` from the response.
                            2. Click **Authorize** → enter: `Bearer <token>`.
                            3. Call `/address/add-address` with a valid JSON body (see examples below).  
                               You’ll get `200 OK` and a JSON with the created/reused address.
                    
                            **400 VALIDATION_ERROR:**
                            - Stay authorized.
                            - Send an invalid or incomplete JSON (e.g., missing `"postalCode"` or malformed JSON).  
                              You’ll get `400` with `code: VALIDATION_ERROR` or `MESSAGE_NOT_READABLE`.
                    
                            **401 UNAUTHORIZED:**
                            - Click **Authorize → Logout** (remove Bearer token) → enter: Bearer <any invalid access_token, for example, ghjggjgkkgkg>. 
                            - Call `/address/add-address` again — you’ll get `401 Unauthorized`.
                    
                            **403 FORBIDDEN:**
                            - Call `/auth/login` as an admin (`ROLE_ADMIN`).  
                            - Authorize with that token.  
                            - Call `/address/add-address` — admins are not allowed; you’ll get `403 Forbidden`.
                    
                            **423 ACCOUNT_LOCKED:**
                            1. Login as a normal user → Authorize → `/auth/dev/_lock?username=<that_user>`.
                            2. Call `/address/add-address` — you’ll get `423 Locked`.
                            3. To fix it → `/auth/dev/_unlock?username=<that_user>`.
                    
                            **Notes:**
                            - Dev endpoints `/auth/dev/_lock` and `/_unlock` are available only when `demo.helpers.enabled=true`.
                            - This endpoint requires a valid JWT access token.
                            - The address is either created or reused if identical entry exists.
                            """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Address data",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AddressRequestDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Valid address",
                                            summary = "All fields provided and valid",
                                            value = """
                                                    {
                                                      "city": "Berlin",
                                                      "street": "Main Street",
                                                      "houseNumber": "12A",
                                                      "apartment": "45",
                                                      "postalCode": "10115",
                                                      "deliveryType": "POSTAL",
                                                      "housingType": "APARTMENT"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Invalid address (validation error)",
                                            summary = "Fails @Valid/@Pattern/@NotNull checks",
                                            value = """
                                                    {
                                                      "city": "berlin",
                                                      "street": "",
                                                      "houseNumber": "???",
                                                      "apartment": null,
                                                      "postalCode": "not-zip",
                                                      "deliveryType": "POSTAL"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address linked to the user",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AddressResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "OK",
                                    summary = "Address successfully created or reused",
                                    value = """
                                            {
                                              "city": "Berlin",
                                              "street": "Main Street",
                                              "houseNumber": "12A",
                                              "apartment": "45",
                                              "postalCode": "10115"
                                            }
                                            """
                        ))),

            @ApiResponse(responseCode = "400", description = "Bad Request",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "VALIDATION_ERROR",
                                            summary = "DTO validation failed",
                                            value = """
                                                    {
                                                      "status": 400,
                                                      "code": "VALIDATION_ERROR",
                                                      "message": "city - must start with a capital letter; street - must not be blank; apartment - must not be null; postalCode - invalid format;",
                                                      "path": "/address/add-address"
                                                    }
                                                    """),
                                    @ExampleObject(
                                            name = "MESSAGE_NOT_READABLE",
                                            summary = "Malformed JSON / wrong type",
                                            value = """
                                                    {
                                                      "status": 400,
                                                      "code": "MESSAGE_NOT_READABLE",
                                                      "message": "JSON parse error: Unexpected end-of-input",
                                                      "path": "/address/add-address"
                                                    }
                                                    """)
                                    })),

            @ApiResponse(responseCode = "401", description = "Unauthorized (no authentication)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "UNAUTHORIZED",
                                    summary = "Spring Security entry point (not GlobalExceptionHandler)",
                                    value = """
                                            {
                                              "status": 401,
                                              "code": "UNAUTHORIZED",
                                              "message": "Authentication is required to access this resource",
                                              "path": "/address/add-address"
                                            }
                                            """
            ))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "ACCESS_DENIED",
                                    summary = "Authenticated but lacks required authority",
                                    value = """
                                            {
                                              "status": 403,
                                              "code": "ACCESS_DENIED",
                                              "message": "Access is denied",
                                              "path": "/address/add-address"
                                            }
                                            """
                            ))),
            @ApiResponse(responseCode = "423", description = "Account is locked/deactivated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "ACCOUNT_LOCKED",
                                    summary = "LockedException mapping",
                                    value = """
                                            {
                                              "status": 423,
                                              "code": "ACCOUNT_LOCKED",
                                              "message": "Your account is deactivated. Would you like to restore it?",
                                              "path": "/address/add-address"
                                            }
                                            """
                            ))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "INTERNAL_ERROR",
                                    summary = "Unhandled exception",
                                    value = """
                                            {
                                              "status": 500,
                                              "code": "INTERNAL_ERROR",
                                              "message": "Internal server error",
                                              "path": "/address/add-address"
                                            }
                                            """
                    )))
    })
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
    @PatchMapping(value = "/update-address")
    public ResponseEntity<AddressResponseDTO> updateAddress(@RequestBody @Valid AddressRequestDTO dto,
                                                            BindingResult bindingResult) {
        if (bindingResult.hasErrors())
            ErrorUtil.returnErrorsToClient(bindingResult);

        int userId = getUserId();
        int addressId = peopleService.getAddressIdByPersonId(userId);

        AddressResponseDTO updatedAddress = addressService.updateAddress(addressId, dto);
        return ResponseEntity.ok(updatedAddress);
    }

    public int getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return ((PersonDetails) authentication.getPrincipal()).getId();
    }
}
