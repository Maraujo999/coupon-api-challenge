package com.maraujo.couponapi.coupon.application.service;

import com.maraujo.couponapi.coupon.application.command.CreateCouponCommand;
import com.maraujo.couponapi.coupon.application.port.in.CreateCouponUseCase;
import com.maraujo.couponapi.coupon.application.port.out.CouponRepository;
import com.maraujo.couponapi.coupon.domain.model.Coupon;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

public final class CreateCouponService implements CreateCouponUseCase {
  private final CouponRepository repository;
  private final Clock clock;
  private final Supplier<UUID> identifiers;

  public CreateCouponService(CouponRepository repository, Clock clock, Supplier<UUID> identifiers) {
    this.repository = repository;
    this.clock = clock;
    this.identifiers = identifiers;
  }

  @Override
  public Coupon execute(CreateCouponCommand command) {
    var coupon =
        Coupon.create(
            identifiers.get(),
            command.code(),
            command.description(),
            command.discountValue(),
            command.expirationDate(),
            command.published(),
            clock.instant());
    repository.insert(coupon);
    return coupon;
  }
}
