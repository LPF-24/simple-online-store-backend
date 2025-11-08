package com.simple_online_store_backend.dto.code;

import com.simple_online_store_backend.util.SwaggerConstants;
import com.simple_online_store_backend.validation.annotation.ValidCode;
import io.swagger.v3.oas.annotations.media.Schema;

public class CodeRequestDTO {
    @ValidCode
    @Schema(description = SwaggerConstants.SPECIAL_CODE_DESC)
    private String code;

    public CodeRequestDTO(String code) {
        this.code = code;
    }

    public CodeRequestDTO() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
