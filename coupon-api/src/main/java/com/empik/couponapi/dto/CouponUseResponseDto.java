package com.empik.couponapi.dto;

import com.empik.shared.enums.CouponUseStatusEnum;

public class CouponUseResponseDto extends BaseResponseDto {

    private CouponUseStatusEnum status;

    public CouponUseStatusEnum getStatus() {
        return status;
    }

    public void setStatus(CouponUseStatusEnum status) {
        this.status = status;
    }
}
