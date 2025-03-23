package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.dto.JwtResponse;
import com.simple_online_store_backend.dto.LoginRequest;
import com.simple_online_store_backend.dto.PersonRequestDTO;
import com.simple_online_store_backend.exception.ErrorUtil;
import com.simple_online_store_backend.security.JWTUtil;
import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.PeopleService;
import com.simple_online_store_backend.service.PersonDetailsService;
import com.simple_online_store_backend.util.PersonValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final JWTUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final PeopleService peopleService;
    private final PersonValidator personValidator;

    @Autowired
    public AuthController(JWTUtil jwtUtil, AuthenticationManager authenticationManager, PeopleService peopleService, PersonValidator personValidator) {
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.peopleService = peopleService;
        this.personValidator = personValidator;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),
                            loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();
            String role = personDetails.getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .orElse("ROLE_USER");

            String jwt = jwtUtil.generateToken(personDetails.getUsername(), role);

            return ResponseEntity.ok(new JwtResponse(jwt, personDetails.getId(), personDetails.getUsername()));
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(Map.of("message", "Your account is deactivated. Would you like to restore it?"));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new JwtResponse(null, null, "Invalid username or password"));
        }
    }

    @PostMapping("/registration")
    public ResponseEntity<HttpStatus> performRegistration(@RequestBody @Valid PersonRequestDTO dto,
                                                          BindingResult bindingResult) {
        System.out.println("Beginning of the method performRegistration");
        personValidator.validate(dto, bindingResult);
        System.out.println("Validation with personValidator was successful");
        if (bindingResult.hasErrors())
            ErrorUtil.returnErrorsToClient(bindingResult);
        System.out.println("Middle of the method");

        peopleService.register(dto);
        System.out.println("End of the method performRegistration");
        return ResponseEntity.ok(HttpStatus.OK);
    }

}
