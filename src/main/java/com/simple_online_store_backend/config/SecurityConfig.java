package com.simple_online_store_backend.config;

import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.PersonDetailsService;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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

    public SecurityConfig(JWTFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    /**
     * Authentication provider bean.
     * It hooks up PersonDetailsService and PasswordEncoder.
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
     * AuthenticationManager bean - so you can inject it into AuthController.
     * In modern versions of Spring Security, we get the builder from HttpSecurity,
     * register our DaoAuthenticationProvider in it and build the manager.
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http,
                                                       DaoAuthenticationProvider daoAuthenticationProvider) throws Exception {
        // create an AuthenticationManagerBuilder that manages authentication providers
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                // use the previously created daoAuthenticationProvider
                .authenticationProvider(daoAuthenticationProvider)
                .build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   DaoAuthenticationProvider daoAuthenticationProvider) throws Exception {
        return http
                .cors(Customizer.withDefaults()) // enable cross-site request settings manually
                .csrf(AbstractHttpConfigurer::disable) // disable CSRF
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Swagger 3/OpenAPI 3 paths - allow access
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        ).permitAll()
                        .requestMatchers("/auth/login", "/auth/registration", "/auth/refresh", "/auth/logout", "/error",
                                "/people/all-customers", "/people/restore-account", "/product").permitAll()
                        .requestMatchers("/pickup/all-pickup-location", "/orders/{id}", "/product/all-active-products").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        .requestMatchers("/address/add-address", "/address/update-address",
                                "/people/deactivate-account", "/orders/create-order", "/orders/all-my-orders", "/orders/{id}/cancel-order",
                                "/orders/{id}/reactivate-order").hasAuthority("ROLE_USER")
                        .requestMatchers("/product/add-product", "/product/{id}/update-product",
                                "/pickup/add-pickup-location", "/pickup/{id}/close-pick-up-location", "/pickup/{id}/open-pick-up-location",
                                "/pickup/{id}/update-pick-up-location", "/orders").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/people/{id}/profile").access((authentication, request) -> {
                            String requestUri = request.getRequest().getRequestURI();
                            String userId = requestUri.replaceAll("\\D+", "");

                            PersonDetails personDetails = (PersonDetails) authentication.get().getPrincipal();
                            String currentUserId = String.valueOf(personDetails.getId());

                            boolean isOwner = userId.equals(currentUserId);
                            boolean isAdmin = personDetails.getAuthorities().stream()
                                    .anyMatch(authentic -> authentic.getAuthority().equals("ROLE_ADMIN"));

                            return new AuthorizationDecision(isOwner || isAdmin);
                        })
                        .anyRequest().authenticated()
                )
                /*
                Register the provider (you don't have to specify it if you're already passing everything to authenticationManager;
                 but usually duplications don't interfere)
                 */
                .authenticationProvider(daoAuthenticationProvider)

                // Embed a JWT filter that checks the Authorization header
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
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
