package com.maraujo.couponapi.coupon.application.exception;

public final class ConcurrentCouponModificationException extends RuntimeException {
  public ConcurrentCouponModificationException() {
    super("O cupom foi alterado por outra operação. Consulte seu estado atual.");
  }
}
