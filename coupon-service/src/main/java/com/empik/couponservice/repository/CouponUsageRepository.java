package com.empik.couponservice.repository;

import com.empik.couponservice.domain.Coupon;
import com.empik.couponservice.domain.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, UUID> {

    boolean existsByCouponAndUserId(Coupon coupon, String userId);
}
