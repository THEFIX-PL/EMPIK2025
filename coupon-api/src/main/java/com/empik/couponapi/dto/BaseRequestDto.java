package com.empik.couponapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BaseRequestDto {

    @NotBlank(message = "Coupon code cannot be blank")
    @Size(min = 1, max = 64, message = "Coupon code must be between 1 and 64 characters")
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
