package com.maraujo.couponapi.coupon.application.port.out;

import com.maraujo.couponapi.coupon.domain.model.Coupon;
import java.util.Optional;
import java.util.UUID;

public interface CouponRepository {
  void insert(Coupon coupon);

  Optional<StoredCoupon> findById(UUID id);

  void update(Coupon coupon, long expectedVersion);
}
