package com.maraujo.couponapi.coupon.domain.model;

import com.maraujo.couponapi.coupon.domain.exception.BusinessRuleViolation;
import com.maraujo.couponapi.coupon.domain.exception.RuleCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Coupon {
  private final CouponSnapshot snapshot;

  private Coupon(CouponSnapshot snapshot) {
    this.snapshot = Objects.requireNonNull(snapshot);
    Objects.requireNonNull(snapshot.id());
    Objects.requireNonNull(snapshot.code());
    Objects.requireNonNull(snapshot.discountValue());
    Objects.requireNonNull(snapshot.expirationDate());
    Objects.requireNonNull(snapshot.status());
    Objects.requireNonNull(snapshot.createdAt());
    validateDescription(snapshot.description());
    if ((snapshot.status() == CouponStatus.DELETED) != (snapshot.deletedAt() != null)) {
      throw new BusinessRuleViolation(
          RuleCode.INVALID_COUPON_STATE, "Estado e data de exclusão inconsistentes.");
    }
  }

  public static Coupon create(
      UUID id,
      String code,
      String description,
      BigDecimal discountValue,
      Instant expirationDate,
      boolean published,
      Instant now) {
    Objects.requireNonNull(now);
    if (expirationDate == null) {
      throw new BusinessRuleViolation(
          RuleCode.INVALID_EXPIRATION_DATE, "A expiração é obrigatória.");
    }
    if (expirationDate.isBefore(now)) {
      throw new BusinessRuleViolation(
          RuleCode.EXPIRATION_IN_PAST, "A expiração não pode estar no passado.");
    }
    return new Coupon(
        new CouponSnapshot(
            id,
            new CouponCode(code),
            description,
            new DiscountValue(discountValue),
            expirationDate,
            CouponStatus.ACTIVE,
            published,
            false,
            now,
            null));
  }

  public static Coupon restore(CouponSnapshot snapshot) {
    return new Coupon(snapshot);
  }

  public CouponSnapshot snapshot() {
    return snapshot;
  }

  public boolean isDeleted() {
    return snapshot.status() == CouponStatus.DELETED;
  }

  public Coupon delete(Instant now) {
    Objects.requireNonNull(now);
    if (isDeleted()) {
      throw new BusinessRuleViolation(RuleCode.COUPON_ALREADY_DELETED, "O cupom já foi excluído.");
    }
    if (now.isBefore(snapshot.createdAt())) {
      throw new BusinessRuleViolation(
          RuleCode.INVALID_COUPON_STATE, "A exclusão não pode preceder a criação.");
    }
    return new Coupon(
        new CouponSnapshot(
            snapshot.id(),
            snapshot.code(),
            snapshot.description(),
            snapshot.discountValue(),
            snapshot.expirationDate(),
            CouponStatus.DELETED,
            snapshot.published(),
            snapshot.redeemed(),
            snapshot.createdAt(),
            now));
  }

  private static void validateDescription(String description) {
    if (description == null || description.isBlank()) {
      throw new BusinessRuleViolation(
          RuleCode.INVALID_COUPON_DESCRIPTION, "A descrição não pode estar em branco.");
    }
  }
}
