package com.simple_online_store_backend.dto.person;

import com.simple_online_store_backend.validation.annotation.ValidDateOfBirth;
import com.simple_online_store_backend.validation.annotation.ValidPassword;
import com.simple_online_store_backend.validation.annotation.ValidPhoneNumber;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@NoArgsConstructor
@Getter
@Setter
public class PersonRequestDTO {
    @NotEmpty(message = "Username can't be empty")
    @Size(min = 2, max = 100, message = "Username must be between 2 and 100 characters long")
    private String userName;

    @NotEmpty(message = "Password can't be empty")
    @ValidPassword
    private String password;

    @ValidDateOfBirth
    private LocalDate dateOfBirth;

    @ValidPhoneNumber
    private String phoneNumber;

    @NotEmpty(message = "Email can't be empty")
    @Email(message = "Email should be valid")
    @Size(max = 50, message = "Email can contain a maximum of 50 characters")
    private String email;

    @NotNull(message = "You must accept the terms and conditions")
    private Boolean agreementAccepted;
}
