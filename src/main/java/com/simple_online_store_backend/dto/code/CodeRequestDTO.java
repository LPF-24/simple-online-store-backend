package com.simple_online_store_backend.dto.code;

import com.simple_online_store_backend.validation.annotation.ValidCode;

public class CodeRequestDTO {
    @ValidCode
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
