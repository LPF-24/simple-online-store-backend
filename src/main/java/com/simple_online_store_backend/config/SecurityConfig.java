package com.simple_online_store_backend.config;

import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.PersonDetailsService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JWTFilter jwtFilter;

    // Конструктор только для тех зависимостей, которые действительно нужны как поля
    public SecurityConfig(JWTFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    /**
     * Бин провайдера аутентификации.
     * Он подключает PersonDetailsService и PasswordEncoder.
     */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(PersonDetailsService personDetailsService,
                                                               PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(personDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    /**
     * Бин AuthenticationManager – чтобы вы могли внедрить его в AuthController.
     * В современных версиях Spring Security получаем билдер из HttpSecurity,
     * регистрируем в нём наш DaoAuthenticationProvider и строим менеджер.
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http,
                                                       DaoAuthenticationProvider daoAuthenticationProvider) throws Exception {
        //создаёт AuthenticationManagerBuilder, который управляет провайдерами аутентификации
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                //используем созданный ранее daoAuthenticationProvider
                .authenticationProvider(daoAuthenticationProvider)
                .build();
    }

    /**
     * Настраиваем SecurityFilterChain: ставим цепочку фильтров, в том числе JWTFilter.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   DaoAuthenticationProvider daoAuthenticationProvider) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable) // отключаем CSRF
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login", "/auth/registration", "/error", "/people/all-customers", "/people/restore-account").permitAll()
                        .requestMatchers("/address/add-address", "/address/update-address", "/people/deactivate-account").hasAuthority("ROLE_USER")
                        .requestMatchers("/people/{id}/profile").access((authentication, request) -> {
                            String requestUri = request.getRequest().getRequestURI();
                            // извлекаем userId из URL
                            String userId = requestUri.replaceAll("\\D+", "");

                            // получаем principal (PersonDetails) из SecurityContext
                            PersonDetails personDetails = (PersonDetails) authentication.get().getPrincipal();
                            String currentUserId = String.valueOf(personDetails.getId());

                            boolean isOwner = userId.equals(currentUserId);
                            return new AuthorizationDecision(isOwner);
                        })
                        .anyRequest().authenticated()
                )
                // Регистрируем провайдер (можно не указывать, если
                // уже всё передаём в authenticationManager; но обычно дубликации не мешают)
                .authenticationProvider(daoAuthenticationProvider)

                // Встраиваем наш JWT фильтр, который проверяет заголовок Authorization
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    /**
     * Кодировщик паролей (нужен для DaoAuthenticationProvider).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Ваши вспомогательные бины.
     */
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        return new MethodValidationPostProcessor();
    }
}
