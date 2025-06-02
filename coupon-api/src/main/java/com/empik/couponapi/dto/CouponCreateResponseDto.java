package com.empik.couponapi.dto;

import com.empik.shared.enums.CouponCreateStatusEnum;

public class CouponCreateResponseDto extends BaseResponseDto {

    private CouponCreateStatusEnum status;

    public CouponCreateStatusEnum getStatus() {
        return status;
    }

    public void setStatus(CouponCreateStatusEnum status) {
        this.status = status;
    }
}