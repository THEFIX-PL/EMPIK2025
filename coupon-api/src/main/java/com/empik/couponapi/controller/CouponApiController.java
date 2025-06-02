package com.empik.couponapi.controller;

import com.empik.couponapi.dto.CouponCreateRequestDto;
import com.empik.couponapi.dto.CouponCreateResponseDto;
import com.empik.couponapi.dto.CouponUseRequestDto;
import com.empik.couponapi.dto.CouponUseResponseDto;
import com.empik.couponapi.service.CouponApiService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.empik.couponapi.util.IpUtil.getClientIpAddress;


@RestController
public class CouponApiController {

    private final CouponApiService couponApiService;
    Logger logger = LoggerFactory.getLogger(CouponApiController.class);

    @Autowired
    public CouponApiController(CouponApiService couponApiService) {
        this.couponApiService = couponApiService;
    }

    @PostMapping
    public ResponseEntity<CouponCreateResponseDto> createCoupon(@Valid @RequestBody CouponCreateRequestDto request) {
        try {
            logger.debug("Received create coupon request: " + request.toString());
            CouponCreateResponseDto response = couponApiService.createCoupon(request);
            logger.info("Create coupon request processed - RequestId: {}, Status: {}",
                    response.getRequestId(), response.getStatus());


            HttpStatus status = switch (response.getStatus()) {
                case CREATED -> HttpStatus.CREATED;
                case ALREADY_EXISTS -> HttpStatus.CONFLICT;
                case FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
                default -> HttpStatus.ACCEPTED;
            };

            return ResponseEntity.status(status).body(response);
        } catch (Exception e) {
            logger.error("Error processing create coupon request - Code: {}", request.getCode(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/use")
    public ResponseEntity<CouponUseResponseDto> useCoupon(@Valid @RequestBody CouponUseRequestDto request,
                                                          HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            logger.info("Received use coupon request - Code: {}, UserId: {}, IP: {}",
                    request.getCode(), request.getUserId(), ipAddress);

            CouponUseResponseDto response = couponApiService.useCoupon(request, ipAddress);

            logger.info("Use coupon request processed - RequestId: {}, Status: {}",
                    response.getRequestId(), response.getStatus());

            HttpStatus status = switch (response.getStatus()) {
                case SUCCESS -> HttpStatus.OK;
                case LIMIT_REACHED -> HttpStatus.BAD_REQUEST;
                case ALREADY_USED -> HttpStatus.BAD_REQUEST;
                case COUNTRY_NOT_SUPPORTED -> HttpStatus.BAD_REQUEST;
                case COUNTRY_ERROR -> HttpStatus.BAD_REQUEST;
                case NOT_EXISTS -> HttpStatus.NOT_FOUND;
                case FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
                case PENDING -> HttpStatus.ACCEPTED;
                default -> HttpStatus.ACCEPTED;
            };

            return ResponseEntity.status(status).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/status/{requestId}")
    public ResponseEntity<CouponCreateResponseDto> getCreateTaskStatus(@PathVariable UUID requestId) {
        try {
            logger.info("Checking create coupon task status - RequestId: {}", requestId);

            CouponCreateResponseDto response = couponApiService.getCreateTaskStatus(requestId);

            logger.debug("Create coupon task status retrieved - RequestId: {}, Status: {}",
                    requestId, response.getStatus());

            HttpStatus status = switch (response.getStatus()) {
                case CREATED -> HttpStatus.CREATED;
                case ALREADY_EXISTS -> HttpStatus.CONFLICT;
                case FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
                case PENDING -> HttpStatus.ACCEPTED;
                default -> HttpStatus.ACCEPTED;
            };

            return ResponseEntity.status(status).body(response);
        } catch (Exception e) {
            logger.error("Error retrieving create coupon task status - RequestId: {}", requestId, e);
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/use/status/{requestId}")
    public ResponseEntity<CouponUseResponseDto> getUseTaskStatus(@PathVariable UUID requestId) {

        try {
            logger.info("Checking use coupon task status - RequestId: {}", requestId);

            CouponUseResponseDto response = couponApiService.getUseTaskStatus(requestId);

            logger.debug("Use coupon task status retrieved - RequestId: {}, Status: {}",
                    requestId, response.getStatus());

            HttpStatus status = switch (response.getStatus()) {
                case SUCCESS -> HttpStatus.OK;
                case LIMIT_REACHED -> HttpStatus.BAD_REQUEST;
                case ALREADY_USED -> HttpStatus.BAD_REQUEST;
                case COUNTRY_NOT_SUPPORTED -> HttpStatus.BAD_REQUEST;
                case COUNTRY_ERROR -> HttpStatus.BAD_REQUEST;
                case NOT_EXISTS -> HttpStatus.NOT_FOUND;
                case FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
                default -> HttpStatus.ACCEPTED;
            };

            return ResponseEntity.status(status).body(response);
        } catch (Exception e) {
            logger.error("Error retrieving create coupon task status - RequestId: {}", requestId, e);
            throw new RuntimeException(e);
        }
    }
}