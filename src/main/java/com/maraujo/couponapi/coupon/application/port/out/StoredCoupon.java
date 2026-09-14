package com.maraujo.couponapi.coupon.application.port.out;

import com.maraujo.couponapi.coupon.domain.model.Coupon;

public record StoredCoupon(Coupon coupon, long version) {}
