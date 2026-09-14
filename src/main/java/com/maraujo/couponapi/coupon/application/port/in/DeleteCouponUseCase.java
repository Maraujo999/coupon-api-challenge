package com.maraujo.couponapi.coupon.application.port.in;

import java.util.UUID;

@FunctionalInterface
public interface DeleteCouponUseCase {
  void execute(UUID id);
}
