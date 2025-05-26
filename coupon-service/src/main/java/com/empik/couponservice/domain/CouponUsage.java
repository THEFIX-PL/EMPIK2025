package com.empik.couponservice.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "coupon_usages",
        indexes = {
                @Index(name = "idx_user_id", columnList = "userId"),
                @Index(name = "idx_coupon_id", columnList = "coupon_id"),
                @Index(name = "idx_used_at", columnList = "usedAt"),
                @Index(name = "idx_user_country_code", columnList = "userCountryCode")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_coupon", columnNames = {"userId", "coupon_id"})
        }
)
public class CouponUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false, foreignKey = @ForeignKey(name = "fk_usage_coupon"))
    private Coupon coupon;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;

    @Column(name = "user_country_code", length = 2)
    private String userCountryCode;

    public CouponUsage() {
        // Empty constructor for JPA
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
    }

    public Coupon getCoupon() {
        return coupon;
    }

    public void setCoupon(Coupon coupon) {
        this.coupon = Objects.requireNonNull(coupon, "Coupon cannot be null");
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public String getUserCountryCode() {
        return userCountryCode;
    }

    public void setUserCountryCode(String userCountryCode) {
        this.userCountryCode = userCountryCode;
    }

    @PrePersist
    protected void onCreate() {
        if (usedAt == null) {
            usedAt = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CouponUsage that)) return false;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "CouponUsage{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", coupon=" + coupon +
                ", usedAt=" + usedAt +
                ", userCountryCode='" + userCountryCode + '\'' +
                '}';
    }
}