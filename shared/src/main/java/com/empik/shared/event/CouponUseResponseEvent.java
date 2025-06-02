package com.empik.shared.event;

import com.empik.shared.enums.CouponUseStatusEnum;

import java.util.UUID;

public class CouponUseResponseEvent {

    private UUID requestId;
    private CouponUseStatusEnum status;

    public UUID getRequestId() {
        return requestId;
    }

    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }

    public CouponUseStatusEnum getStatus() {
        return status;
    }

    public void setStatus(CouponUseStatusEnum status) {
        this.status = status;
    }
}