package com.empik.couponapi.exception;

import java.util.UUID;

public class TaskNotFoundException extends CouponApiException {

    public TaskNotFoundException(UUID taskId) {
        super("Task with ID " + taskId + " not found");
    }
}