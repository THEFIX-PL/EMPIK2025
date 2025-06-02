package com.empik.couponapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CouponUseRequestDto extends BaseRequestDto {

    @NotBlank(message = "User ID cannot be blank")
    @Size(max = 64, message = "User ID cannot exceed 64 characters")
    private String userId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}