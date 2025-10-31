package com.simple_online_store_backend.dto.person;

import com.simple_online_store_backend.util.SwaggerConstants;
import io.swagger.v3.oas.annotations.media.Schema;

public class PersonShortDTO {
    @Schema(description = SwaggerConstants.ID_DESC + " person", example = SwaggerConstants.ID_EXAMPLE)
    private Integer id;

    @Schema(description = SwaggerConstants.USERNAME_DESC, example = SwaggerConstants.USERNAME_EXAMPLE)
    private String userName;

    public PersonShortDTO() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
