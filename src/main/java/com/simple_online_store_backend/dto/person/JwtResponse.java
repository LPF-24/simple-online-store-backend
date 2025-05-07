package com.simple_online_store_backend.dto.person;

import com.simple_online_store_backend.util.SwaggerConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {
    @Schema(description = "JWT access token", example = "")
    private String token;

    @Schema(description = SwaggerConstants.ID_DESC, example = SwaggerConstants.ID_EXAMPLE)
    private Integer id;

    @Schema(description = SwaggerConstants.USERNAME_DESC, example = SwaggerConstants.USERNAME_EXAMPLE)
    private String username;
}
