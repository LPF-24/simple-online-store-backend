package com.simple_online_store_backend.dto.person;

import com.simple_online_store_backend.util.SwaggerConstants;
import com.simple_online_store_backend.validation.annotation.ValidDateOfBirth;
import com.simple_online_store_backend.validation.annotation.ValidPassword;
import com.simple_online_store_backend.validation.annotation.ValidPhoneNumber;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Schema(description = "Data required for user registration")
public class PersonRequestDTO {

    @Schema(description = SwaggerConstants.USERNAME_DESC, example = SwaggerConstants.USERNAME_EXAMPLE)
    @NotEmpty(message = "Username can't be empty")
    @Size(min = 2, max = 100, message = "Username must be between 2 and 100 characters long")
    private String userName;

    @Schema(description = SwaggerConstants.PASSWORD_DESC,
            example = SwaggerConstants.PASSWORD_EXAMPLE)
    @NotEmpty(message = "Password can't be empty")
    @ValidPassword
    private String password;

    @Schema(description = SwaggerConstants.DATE_OF_BIRTH_DESC,
            example = SwaggerConstants.DATE_OF_BIRTH_EXAMPLE)
    @ValidDateOfBirth
    private LocalDate dateOfBirth;

    @Schema(description = SwaggerConstants.PHONE_NUMBER_DESC, example = SwaggerConstants.PHONE_NUMBER_EXAMPLE)
    @ValidPhoneNumber
    private String phoneNumber;

    @Schema(description = SwaggerConstants.EMAIL_DESC, example = SwaggerConstants.EMAIL_EXAMPLE)
    @NotEmpty(message = "Email can't be empty")
    @Email(message = "Email should be valid")
    @Size(max = 50, message = "Email can contain a maximum of 50 characters")
    private String email;

    @Schema(description = SwaggerConstants.AGREEMENT_ACCEPTED_DESC)
    @NotNull(message = "You must accept the terms and conditions")
    private Boolean agreementAccepted;

    @Schema(description = SwaggerConstants.SPECIAL_CODE_DESC)
    private String specialCode;

    public PersonRequestDTO() {
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getAgreementAccepted() {
        return agreementAccepted;
    }

    public void setAgreementAccepted(Boolean agreementAccepted) {
        this.agreementAccepted = agreementAccepted;
    }

    public String getSpecialCode() {
        return specialCode;
    }

    public void setSpecialCode(String specialCode) {
        this.specialCode = specialCode;
    }
}
