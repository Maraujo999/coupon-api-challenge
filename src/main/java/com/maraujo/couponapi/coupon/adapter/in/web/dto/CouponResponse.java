package com.maraujo.couponapi.coupon.adapter.in.web.dto;

import com.maraujo.couponapi.coupon.domain.model.CouponStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CouponResponse(
    UUID id,
    String code,
    String description,
    BigDecimal discountValue,
    Instant expirationDate,
    CouponStatus status,
    boolean published,
    boolean redeemed) {}
