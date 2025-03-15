package com.simple_online_store_backend.config;

import com.simple_online_store_backend.security.PersonDetails;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserDetailsService userDetailsService) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/people/{id}/profile").access((authentication, request) -> {
                            String requestUri = request.getRequest().getRequestURI();
                            String userId = requestUri.replaceAll("\\D+", "");

                            PersonDetails personDetails = (PersonDetails) authentication.get().getPrincipal();
                            String currentUserId = String.valueOf(personDetails.getId());

                            boolean isOwner = userId.equals(currentUserId);

                            return new AuthorizationDecision(isOwner);
                        })
                        .requestMatchers("/auth/login", "/auth/registration", "/error").permitAll())
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        // Создаем объект DaoAuthenticationProvider, который будет использоваться для аутентификации пользователей
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        // Устанавливаем сервис, который загружает информацию о пользователях (например, из базы данных)
        authProvider.setUserDetailsService(userDetailsService);
        // Указываем, какой кодировщик паролей использовать для сравнения введенного пароля с сохраненным
        authProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(authProvider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        return new MethodValidationPostProcessor();
    }
}
