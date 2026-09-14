package com.maraujo.couponapi.coupon.application.service;

import com.maraujo.couponapi.coupon.application.exception.CouponNotFoundException;
import com.maraujo.couponapi.coupon.application.port.in.GetCouponUseCase;
import com.maraujo.couponapi.coupon.application.port.out.CouponRepository;
import com.maraujo.couponapi.coupon.domain.model.Coupon;
import java.util.UUID;

public final class GetCouponService implements GetCouponUseCase {
  private final CouponRepository repository;

  public GetCouponService(CouponRepository repository) {
    this.repository = repository;
  }

  @Override
  public Coupon execute(UUID id) {
    return repository
        .findById(id)
        .map(stored -> stored.coupon())
        .filter(coupon -> !coupon.isDeleted())
        .orElseThrow(CouponNotFoundException::new);
  }
}
