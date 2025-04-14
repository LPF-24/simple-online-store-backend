package com.simple_online_store_backend.dto.person;

import com.simple_online_store_backend.util.SwaggerConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@Getter
@Setter
public class LoginRequest {
    @Schema(description = SwaggerConstants.USERNAME_DESC, examples = {"john", "test2"})
    private String username;

    @Schema(description = SwaggerConstants.PASSWORD_DESC, examples = {"Test234!", "Test678!"})
    private String password;
}
