package com.maraujo.couponapi.coupon.domain.model;

import java.time.Instant;
import java.util.UUID;

public record CouponSnapshot(
    UUID id,
    CouponCode code,
    String description,
    DiscountValue discountValue,
    Instant expirationDate,
    CouponStatus status,
    boolean published,
    boolean redeemed,
    Instant createdAt,
    Instant deletedAt) {}
