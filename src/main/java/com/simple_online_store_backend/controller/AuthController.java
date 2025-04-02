package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.dto.JwtResponse;
import com.simple_online_store_backend.dto.person.LoginRequest;
import com.simple_online_store_backend.dto.person.PersonRequestDTO;
import com.simple_online_store_backend.exception.ErrorUtil;
import com.simple_online_store_backend.security.JWTUtil;
import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.PeopleService;
import com.simple_online_store_backend.service.PersonDetailsService;
import com.simple_online_store_backend.service.RefreshTokenService;
import com.simple_online_store_backend.util.PersonValidator;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final JWTUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final PeopleService peopleService;
    private final PersonValidator personValidator;
    private final RefreshTokenService refreshTokenService;
    private final PersonDetailsService personDetailsService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
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

            //String jwt = jwtUtil.generateToken(personDetails.getUsername(), role);
            //Генерируем access и refresh токены
            String accessToken = jwtUtil.generateToken(personDetails.getUsername(), role);
            String refreshToken = jwtUtil.generateRefreshToken(personDetails.getUsername());

            //Сохраняем refresh токен в Redis (по username)
            refreshTokenService.saveRefreshToken(personDetails.getUsername(), refreshToken);

            //Устанавливаем refresh токен в HttpOnly Cookie (не будет доступен через JS)
            ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true) //JS не сможет прочитать эту cookie → защита от XSS-атак.
                    .secure(false) //true - только по HTTPS
                    .path("/") //Указывает, что cookie будет отправляться для всех путей сайта (/api, /auth, и т.д.).
                    //Если указать, например, .path("/auth") — cookie будет работать только там.
                    .maxAge(Duration.ofDays(7))
                    .sameSite("Strict") //cookie не отправляется с внешних сайтов (жесткая защита)
                    .build();
            response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString()); //Устанавливает cookie в HTTP-ответе
            // (добавляет заголовок Set-Cookie), чтобы браузер сохранил её,
            // т.к. refresh токен на стороне клиента хранится в памяти браузера

            //возвращаем access токен и ID пользователя в теле ответа
            return ResponseEntity.ok(new JwtResponse(accessToken, personDetails.getId(), personDetails.getUsername()));
        } catch (LockedException e) {
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
        logger.info("Beginning of the method performRegistration");
        personValidator.validate(dto, bindingResult);
        logger.info("Validation with personValidator was successful");
        if (bindingResult.hasErrors())
            ErrorUtil.returnErrorsToClient(bindingResult);
        logger.info("Middle of the method");

        peopleService.register(dto);
        logger.info("End of the method performRegistration");
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue("refreshToken") String refreshToken) {
        try {
            String username = jwtUtil.validateRefreshToken(refreshToken).getClaim("username").asString();
            String role = personDetailsService.loadUserByUsername(username).getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .orElse("ROLE_USER");

            String savedToken = refreshTokenService.getRefreshToken(username);

            if (!refreshToken.equals(savedToken)) {
                throw new RuntimeException("RefreshToken is invalid or expired");
            }

            String newAccessToken = jwtUtil.generateToken(username, role);

            return ResponseEntity.ok(Map.of("access_token", newAccessToken));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid refresh token"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue("refreshToken") String refreshToken) {
        String username = jwtUtil.validateRefreshToken(refreshToken).getClaim("username").asString();
        refreshTokenService.deleteRefreshToken(username);

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(Map.of("message", "Logged out successfully"));
    }
}
