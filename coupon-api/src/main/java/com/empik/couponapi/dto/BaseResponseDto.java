package com.empik.couponapi.dto;

import java.util.UUID;

public class BaseResponseDto {

    private UUID requestId;
    private String message;

    public UUID getRequestId() {
        return requestId;
    }

    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
