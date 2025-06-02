package com.empik.couponapi.service;

import com.empik.couponapi.dto.CouponCreateRequestDto;
import com.empik.couponapi.dto.CouponCreateResponseDto;
import com.empik.couponapi.dto.CouponUseRequestDto;
import com.empik.couponapi.dto.CouponUseResponseDto;
import com.empik.couponapi.exception.TaskNotFoundException;
import com.empik.shared.enums.CouponCreateStatusEnum;
import com.empik.shared.enums.CouponUseStatusEnum;
import com.empik.shared.event.CouponCreateRequestEvent;
import com.empik.shared.event.CouponCreateResponseEvent;
import com.empik.shared.event.CouponUseRequestEvent;
import com.empik.shared.event.CouponUseResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class CouponApiService {

    private static final Logger logger = LoggerFactory.getLogger(CouponApiService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, CouponCreateResponseEvent> createRedisTemplate;
    private final RedisTemplate<String, CouponUseResponseEvent> useRedisTemplate;

    @Value("${kafka.topics.coupon-create-request}")
    private String couponCreateRequestTopic;

    @Value("${kafka.topics.coupon-use-request}")
    private String couponUseRequestTopic;

    @Autowired
    public CouponApiService(KafkaTemplate<String, Object> kafkaTemplate,
                            RedisTemplate<String, CouponCreateResponseEvent> createRedisTemplate,
                            RedisTemplate<String, CouponUseResponseEvent> useRedisTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.createRedisTemplate = createRedisTemplate;
        this.useRedisTemplate = useRedisTemplate;
    }

    public CouponCreateResponseDto createCoupon(CouponCreateRequestDto request) {
        UUID taskId = UUID.randomUUID();

        logger.info("Creating coupon - TaskId: {}, Code: {}, CountryCode: {}",
                taskId, request.getCode(), request.getCountryCode());

        CouponCreateResponseEvent initialEvent = new CouponCreateResponseEvent();
        initialEvent.setRequestId(taskId);
        initialEvent.setStatus(CouponCreateStatusEnum.PENDING);
        storeCreateEvent(taskId, initialEvent);

        CouponCreateRequestEvent event = new CouponCreateRequestEvent();
        event.setRequestId(taskId);
        event.setCode(request.getCode());
        event.setCountryCode(request.getCountryCode());
        event.setMaxUsage(request.getMaxUsage());

        kafkaTemplate.send(couponCreateRequestTopic, request.getCode(), event);
        logger.debug("Sent create coupon request to Kafka - TaskId: {}", taskId);

        for (int i = 0; i < 50; i++) { // 5 seconds
            CouponCreateResponseEvent response = getCreateEvent(taskId);
            if (response != null && response.getStatus() != CouponCreateStatusEnum.PENDING) {
                logger.info("Received create coupon response - TaskId: {}, Status: {}",
                        taskId, response.getStatus());
                return mapToCreateResponse(response);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logger.info("Create coupon request timeout - TaskId: {}", taskId);

        CouponCreateResponseDto response = new CouponCreateResponseDto();
        response.setRequestId(taskId);
        response.setStatus(CouponCreateStatusEnum.PENDING);
        response.setMessage("Coupon creation request submitted");

        return response;
    }

    public CouponUseResponseDto useCoupon(CouponUseRequestDto request, String ipAddress) {
        UUID taskId = UUID.randomUUID();

        logger.info("Using coupon - TaskId: {}, Code: {}, UserId: {}, IP: {}",
                taskId, request.getCode(), request.getUserId(), ipAddress);

        CouponUseResponseEvent initialEvent = new CouponUseResponseEvent();
        initialEvent.setRequestId(taskId);
        initialEvent.setStatus(CouponUseStatusEnum.PENDING);
        storeUseEvent(taskId, initialEvent);

        CouponUseRequestEvent event = new CouponUseRequestEvent();
        event.setRequestId(taskId);
        event.setCode(request.getCode());
        event.setUserId(request.getUserId());
        event.setIpAddress(ipAddress);

        kafkaTemplate.send(couponUseRequestTopic, request.getCode(), event);
        logger.debug("Sent use coupon request to Kafka - TaskId: {}", taskId);

        for (int i = 0; i < 50; i++) {
            CouponUseResponseEvent response = getUseEvent(taskId);
            if (response != null && response.getStatus() != CouponUseStatusEnum.PENDING) {
                logger.info("Received use coupon response - TaskId: {}, Status: {}",
                        taskId, response.getStatus());
                return mapToUseResponse(response);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logger.info("Use coupon request timeout - TaskId: {}", taskId);

        CouponUseResponseDto response = new CouponUseResponseDto();
        response.setRequestId(taskId);
        response.setStatus(CouponUseStatusEnum.PENDING);
        response.setMessage("Coupon use request submitted");

        return response;
    }

    @KafkaListener(topics = "coupon-create-response", groupId = "coupon-service-group")
    public void handleCouponCreateResponse(CouponCreateResponseEvent event) {
        logger.debug("Received create coupon response from Kafka - RequestId: {}, Status: {}",
                event.getRequestId(), event.getStatus());
        storeCreateEvent(event.getRequestId(), event);
    }

    @KafkaListener(topics = "coupon-use-response", groupId = "coupon-service-group")
    public void handleCouponUseResponse(CouponUseResponseEvent event) {
        logger.debug("Received use coupon response from Kafka - RequestId: {}, Status: {}",
                event.getRequestId(), event.getStatus());
        storeUseEvent(event.getRequestId(), event);
    }

    public CouponCreateResponseDto getCreateTaskStatus(UUID taskId) {
        CouponCreateResponseEvent event = getCreateEvent(taskId);
        if (event == null) {
            throw new TaskNotFoundException(taskId);
        }
        return mapToCreateResponse(event);
    }

    public CouponUseResponseDto getUseTaskStatus(UUID taskId) {
        CouponUseResponseEvent event = getUseEvent(taskId);
        if (event == null) {
            throw new TaskNotFoundException(taskId);
        }
        return mapToUseResponse(event);
    }

    private void storeCreateEvent(UUID taskId, CouponCreateResponseEvent event) {
        String key = "create:" + taskId;
        createRedisTemplate.opsForValue().set(key, event, Duration.ofHours(24));
    }

    private void storeUseEvent(UUID taskId, CouponUseResponseEvent event) {
        String key = "use:" + taskId;
        useRedisTemplate.opsForValue().set(key, event, Duration.ofHours(24));
    }

    private CouponCreateResponseEvent getCreateEvent(UUID taskId) {
        String key = "create:" + taskId;
        return createRedisTemplate.opsForValue().get(key);
    }

    private CouponUseResponseEvent getUseEvent(UUID taskId) {
        String key = "use:" + taskId;
        return useRedisTemplate.opsForValue().get(key);
    }

    private CouponCreateResponseDto mapToCreateResponse(CouponCreateResponseEvent event) {
        String message = switch (event.getStatus()) {
            case CREATED -> "Coupon created successfully";
            case ALREADY_EXISTS -> "Coupon with this code already exists";
            case FAILED -> "Failed to create coupon";
            default -> "Coupon creation in progress";
        };

        CouponCreateResponseDto response = new CouponCreateResponseDto();
        response.setRequestId(event.getRequestId());
        response.setStatus(event.getStatus());
        response.setMessage(message);

        return response;
    }

    private CouponUseResponseDto mapToUseResponse(CouponUseResponseEvent event) {
        String message = switch (event.getStatus()) {
            case SUCCESS -> "Coupon used successfully";
            case LIMIT_REACHED -> "Coupon usage limit reached";
            case ALREADY_USED -> "Coupon already used by this user";
            case COUNTRY_NOT_SUPPORTED -> "Coupon not supported in this country";
            case COUNTRY_ERROR -> "Error determining country from IP";
            case NOT_EXISTS -> "Coupon does not exist";
            case FAILED -> "Failed to use coupon";
            default -> "Coupon use in progress";
        };

        CouponUseResponseDto response = new CouponUseResponseDto();
        response.setRequestId(event.getRequestId());
        response.setStatus(event.getStatus());
        response.setMessage(message);

        return response;
    }
}