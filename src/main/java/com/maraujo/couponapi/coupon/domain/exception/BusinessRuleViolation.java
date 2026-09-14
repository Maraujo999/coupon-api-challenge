package com.maraujo.couponapi.coupon.domain.exception;

public final class BusinessRuleViolation extends RuntimeException {
  private final RuleCode code;

  public BusinessRuleViolation(RuleCode code, String message) {
    super(message);
    this.code = code;
  }

  public RuleCode code() {
    return code;
  }
}
