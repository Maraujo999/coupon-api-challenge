package com.maraujo.couponapi.coupon.application.exception;

public final class CouponNotFoundException extends RuntimeException {
  public CouponNotFoundException() {
    super("Cupom não encontrado.");
  }
}
