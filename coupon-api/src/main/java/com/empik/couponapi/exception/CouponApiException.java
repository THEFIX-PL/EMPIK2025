package com.empik.couponapi.exception;

public class CouponApiException extends RuntimeException {

    public CouponApiException(String message) {
        super(message);
    }

    public CouponApiException(String message, Throwable cause) {
        super(message, cause);
    }
}