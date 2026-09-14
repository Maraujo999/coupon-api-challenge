package com.maraujo.couponapi.coupon.application.port.in;

import com.maraujo.couponapi.coupon.domain.model.Coupon;
import java.util.UUID;

@FunctionalInterface
public interface GetCouponUseCase {
  Coupon execute(UUID id);
}
