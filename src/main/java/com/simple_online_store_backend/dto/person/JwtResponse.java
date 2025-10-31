package com.simple_online_store_backend.dto.person;

import com.simple_online_store_backend.util.SwaggerConstants;
import io.swagger.v3.oas.annotations.media.Schema;

public class JwtResponse {
    @Schema(description = "JWT access token", example = "")
    private String token;

    @Schema(description = SwaggerConstants.ID_DESC, example = SwaggerConstants.ID_EXAMPLE)
    private Integer id;

    @Schema(description = SwaggerConstants.USERNAME_DESC, example = SwaggerConstants.USERNAME_EXAMPLE)
    private String username;

    public JwtResponse(String token, Integer id, String username) {
        this.token = token;
        this.id = id;
        this.username = username;
    }

    public JwtResponse() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
