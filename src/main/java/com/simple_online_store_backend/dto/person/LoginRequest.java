package com.simple_online_store_backend.dto.person;

import com.simple_online_store_backend.util.SwaggerConstants;
import io.swagger.v3.oas.annotations.media.Schema;

public class LoginRequest {
    @Schema(description = SwaggerConstants.USERNAME_DESC, examples = {"john", "test2"})
    private String username;

    @Schema(description = SwaggerConstants.PASSWORD_DESC, examples = {"Test234!", "Test678!"})
    private String password;

    public LoginRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
