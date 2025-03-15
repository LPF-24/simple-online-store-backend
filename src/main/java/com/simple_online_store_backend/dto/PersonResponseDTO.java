package com.simple_online_store_backend.dto;

import jakarta.persistence.Column;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PersonResponseDTO {
    private Integer id;

    private String userName;

    private LocalDate dateOfBirth;

    private String phoneNumber;

    private String email;

    private String role;
}
