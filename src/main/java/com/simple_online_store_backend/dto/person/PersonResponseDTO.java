package com.simple_online_store_backend.dto.person;

import com.simple_online_store_backend.util.SwaggerConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PersonResponseDTO {
    @Schema(description = SwaggerConstants.ID_DESC, example = SwaggerConstants.ID_EXAMPLE)
    private Integer id;

    @Schema(description = SwaggerConstants.USERNAME_DESC, example = SwaggerConstants.USERNAME_EXAMPLE)
    private String userName;

    @Schema(description = SwaggerConstants.DATE_OF_BIRTH_DESC, example = SwaggerConstants.DATE_OF_BIRTH_EXAMPLE)
    private LocalDate dateOfBirth;

    @Schema(description = SwaggerConstants.PHONE_NUMBER_DESC, example = SwaggerConstants.PHONE_NUMBER_EXAMPLE)
    private String phoneNumber;

    @Schema(description = SwaggerConstants.EMAIL_DESC, example = SwaggerConstants.EMAIL_EXAMPLE)
    private String email;

    @Schema(description = "The user's role determines the scope of his rights", example = "ROLE_ADMIN")
    private String role;
}
