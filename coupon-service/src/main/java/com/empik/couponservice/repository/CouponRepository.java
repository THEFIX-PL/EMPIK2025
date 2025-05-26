package com.empik.couponservice.repository;

import com.empik.couponservice.domain.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Coupon> findByCodeIgnoreCase(String code);
}
