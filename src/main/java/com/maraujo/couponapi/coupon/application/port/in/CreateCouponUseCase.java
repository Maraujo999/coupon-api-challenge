package com.maraujo.couponapi.coupon.application.port.in;

import com.maraujo.couponapi.coupon.application.command.CreateCouponCommand;
import com.maraujo.couponapi.coupon.domain.model.Coupon;

@FunctionalInterface
public interface CreateCouponUseCase {
  Coupon execute(CreateCouponCommand command);
}
