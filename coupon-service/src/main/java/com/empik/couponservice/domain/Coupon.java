package com.empik.couponservice.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "coupons",
        indexes = {
                @Index(name = "idx_code", columnList = "code"),
                @Index(name = "idx_country_code", columnList = "countryCode")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_code", columnNames = {"code"})
        }
)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 64)
    @NotBlank(message = "Coupon code cannot be blank")
    @Size(max = 64, message = "Coupon code cannot exceed 64 characters")
    private String code;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "max_uses", nullable = false)
    @Min(value = 1, message = "Maximum uses must be at least 1")
    @Max(value = 1000000, message = "Maximum uses cannot exceed 1000000")
    private Integer maxUses;

    @Column(name = "current_uses", nullable = false)
    @Min(value = 0, message = "Current uses cannot be negative")
    private Integer currentUses;

    @Column(name = "country_code", nullable = false, length = 2)
    @NotBlank(message = "Country code cannot be blank")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country code must be 2 uppercase letters")
    private String countryCode;

    public Coupon() {
        // Empty constructor for JPA
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = Objects.requireNonNull(code, "Code cannot be null").toUpperCase();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(Integer maxUses) {
        this.maxUses = Objects.requireNonNull(maxUses, "Max uses cannot be null");
    }

    public Integer getCurrentUses() {
        return currentUses;
    }

    public void setCurrentUses(Integer currentUses) {
        this.currentUses = currentUses;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = Objects.requireNonNull(countryCode, "Country code cannot be null").toUpperCase();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (currentUses == null) {
            currentUses = 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coupon coupon)) return false;
        return Objects.equals(getId(), coupon.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}