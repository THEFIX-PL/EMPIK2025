package com.empik.couponservice.service;

import com.empik.couponservice.domain.Coupon;
import com.empik.couponservice.domain.CouponUsage;
import com.empik.couponservice.repository.CouponRepository;
import com.empik.couponservice.repository.CouponUsageRepository;
import com.empik.shared.enums.CouponCreateStatusEnum;
import com.empik.shared.enums.CouponUseStatusEnum;
import com.empik.shared.event.CouponCreateRequestEvent;
import com.empik.shared.event.CouponCreateResponseEvent;
import com.empik.shared.event.CouponUseRequestEvent;
import com.empik.shared.event.CouponUseResponseEvent;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class CouponService {

    private static final Logger logger = LoggerFactory.getLogger(CouponService.class);

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final IpInfoService ipInfoService;
    private final KafkaTemplate<String, CouponCreateResponseEvent> couponCreateResponseEventKafkaTemplate;
    private final KafkaTemplate<String, CouponUseResponseEvent> couponUseResponseEventKafkaTemplate;
    @Value("${kafka.topics.coupon-create-response}")
    private String couponCreateResponseTopic;
    @Value("${kafka.topics.coupon-use-response}")
    private String couponUseResponseTopic;

    @Autowired
    public CouponService(CouponRepository couponRepository,
                         CouponUsageRepository couponUsageRepository,
                         IpInfoService ipInfoService,
                         KafkaTemplate<String, CouponCreateResponseEvent> couponCreateResponseEventKafkaTemplate,
                         KafkaTemplate<String, CouponUseResponseEvent> couponUseResponseEventKafkaTemplate
    ) {
        this.couponRepository = couponRepository;
        this.couponUsageRepository = couponUsageRepository;
        this.ipInfoService = ipInfoService;
        this.couponCreateResponseEventKafkaTemplate = couponCreateResponseEventKafkaTemplate;
        this.couponUseResponseEventKafkaTemplate = couponUseResponseEventKafkaTemplate;
    }

    @KafkaListener(topics = "coupon-create-request", groupId = "coupon-service-group")
    @Transactional
    public void consumeCouponCreateRequestEvent(@Header(value = KafkaHeaders.KEY, required = false) String code,
                                                CouponCreateRequestEvent event) {
        logger.info("Received coupon create request - RequestId: {}, Code: {}", event.getRequestId(), event.getCode());

        boolean couponExists = couponRepository.existsByCodeIgnoreCase(event.getCode());

        if (couponExists) {
            logger.warn("Coupon with code {} already exists - RequestId: {}", event.getCode(), event.getRequestId());
            sendCouponCreateResponse(event.getRequestId(), CouponCreateStatusEnum.ALREADY_EXISTS);
            return;
        }

        try {
            Coupon coupon = new Coupon();
            coupon.setCode(event.getCode());
            coupon.setCountryCode(event.getCountryCode());
            coupon.setMaxUses(event.getMaxUsage());
            couponRepository.save(coupon);

            logger.info("Successfully created coupon - RequestId: {}, Code: {}, CountryCode: {}",
                    event.getRequestId(), event.getCode(), event.getCountryCode());
            sendCouponCreateResponse(event.getRequestId(), CouponCreateStatusEnum.CREATED);
        } catch (Exception e) {
            logger.error("Error creating coupon - RequestId: {}, Code: {}", event.getRequestId(), event.getCode(), e);
            sendCouponCreateResponse(event.getRequestId(), CouponCreateStatusEnum.FAILED);
        }
    }

    @KafkaListener(topics = "coupon-use-request", groupId = "coupon-service-group")
    @Transactional
    public void consumeCouponUseRequestEvent(@Header(value = KafkaHeaders.KEY, required = false) String code,
                                             CouponUseRequestEvent event) {

        logger.info("Received coupon use request - RequestId: {}, Code: {}, UserId: {}",
                event.getRequestId(), event.getCode(), event.getUserId());

        Optional<Coupon> couponOptional = couponRepository.findByCodeIgnoreCase(event.getCode());

        if (couponOptional.isEmpty()) {
            logger.warn("Coupon not found - RequestId: {}, Code: {}", event.getRequestId(), event.getCode());
            sendCouponUseResponse(event.getRequestId(), CouponUseStatusEnum.NOT_EXISTS);
            return;
        }

        Coupon coupon = couponOptional.get();

        if (coupon.getCurrentUses() >= coupon.getMaxUses()) {
            logger.warn("Coupon usage limit reached - RequestId: {}, Code: {}", event.getRequestId(), event.getCode());
            sendCouponUseResponse(event.getRequestId(), CouponUseStatusEnum.LIMIT_REACHED);
            return;
        }

        boolean couponUsed = couponUsageRepository.existsByCouponAndUserId(coupon, event.getUserId());
        if (couponUsed) {
            logger.warn("Coupon already used by user - RequestId: {}, Code: {}, UserId: {}",
                    event.getRequestId(), event.getCode(), event.getUserId());
            sendCouponUseResponse(event.getRequestId(), CouponUseStatusEnum.ALREADY_USED);
            return;
        }

        String countryCode = ipInfoService.getCountryCodeByIp(event.getIpAddress());
        if (countryCode == null) {
            logger.error("Could not determine country code - RequestId: {}, IP: {}",
                    event.getRequestId(), event.getIpAddress());
            sendCouponUseResponse(event.getRequestId(), CouponUseStatusEnum.COUNTRY_ERROR);
            return;
        }

        if (!countryCode.equalsIgnoreCase(coupon.getCountryCode())) {
            logger.warn("Country not supported - RequestId: {}, Expected: {}, Actual: {}",
                    event.getRequestId(), coupon.getCountryCode(), countryCode);
            sendCouponUseResponse(event.getRequestId(), CouponUseStatusEnum.COUNTRY_NOT_SUPPORTED);
            return;
        }

    try {
        CouponUsage couponUsage = new CouponUsage();
        couponUsage.setCoupon(coupon);
        couponUsage.setUserId(event.getUserId());
        couponUsage.setUserCountryCode(countryCode);
        couponUsageRepository.save(couponUsage);

        coupon.setCurrentUses(coupon.getCurrentUses() + 1);
        couponRepository.save(coupon);

        logger.info("Successfully used coupon - RequestId: {}, Code: {}, UserId: {}, CountryCode: {}",
                event.getRequestId(), event.getCode(), event.getUserId(), countryCode);
        sendCouponUseResponse(event.getRequestId(), CouponUseStatusEnum.SUCCESS);
    } catch (Exception e) {
        logger.error("Error processing coupon use - RequestId: {}, Code: {}",
                event.getRequestId(), event.getCode(), e);
        sendCouponUseResponse(event.getRequestId(), CouponUseStatusEnum.FAILED);
    }

    }

    private void sendCouponCreateResponse(UUID requestId, CouponCreateStatusEnum status) {
        CouponCreateResponseEvent event = new CouponCreateResponseEvent();
        event.setRequestId(requestId);
        event.setStatus(status);

        couponCreateResponseEventKafkaTemplate.send(couponCreateResponseTopic, requestId.toString(), event);
    }

    private void sendCouponUseResponse(UUID requestId, CouponUseStatusEnum status) {
        CouponUseResponseEvent event = new CouponUseResponseEvent();
        event.setRequestId(requestId);
        event.setStatus(status);

        couponUseResponseEventKafkaTemplate.send(couponUseResponseTopic, event);
    }
}