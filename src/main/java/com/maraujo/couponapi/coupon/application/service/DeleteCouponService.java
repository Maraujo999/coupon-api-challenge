package com.maraujo.couponapi.coupon.application.service;

import com.maraujo.couponapi.coupon.application.exception.CouponNotFoundException;
import com.maraujo.couponapi.coupon.application.port.in.DeleteCouponUseCase;
import com.maraujo.couponapi.coupon.application.port.out.CouponRepository;
import java.time.Clock;
import java.util.UUID;

public final class DeleteCouponService implements DeleteCouponUseCase {
  private final CouponRepository repository;
  private final Clock clock;

  public DeleteCouponService(CouponRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Override
  public void execute(UUID id) {
    var stored = repository.findById(id).orElseThrow(CouponNotFoundException::new);
    var deleted = stored.coupon().delete(clock.instant());
    repository.update(deleted, stored.version());
  }
}
