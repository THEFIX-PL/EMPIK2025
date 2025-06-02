package com.empik.shared.event;

import com.empik.shared.enums.CouponCreateStatusEnum;

import java.util.UUID;

public class CouponCreateResponseEvent {

    private UUID requestId;
    private CouponCreateStatusEnum status;

    public UUID getRequestId() {
        return requestId;
    }

    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }

    public CouponCreateStatusEnum getStatus() {
        return status;
    }

    public void setStatus(CouponCreateStatusEnum status) {
        this.status = status;
    }
}
