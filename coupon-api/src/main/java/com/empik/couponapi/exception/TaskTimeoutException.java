package com.empik.couponapi.exception;

import java.util.UUID;

public class TaskTimeoutException extends CouponApiException {

    public TaskTimeoutException(UUID taskId) {
        super("Task with ID " + taskId + " timed out");
    }
}