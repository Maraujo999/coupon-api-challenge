package com.maraujo.couponapi.coupon.domain.model;

import com.maraujo.couponapi.coupon.domain.exception.BusinessRuleViolation;
import com.maraujo.couponapi.coupon.domain.exception.RuleCode;

public record CouponCode(String value) {
  public CouponCode {
    if (value == null) {
      throw invalidCode();
    }
    value = value.replaceAll("[^a-zA-Z0-9]", "");
    if (value.length() != 6) {
      throw invalidCode();
    }
  }

  private static BusinessRuleViolation invalidCode() {
    return new BusinessRuleViolation(
        RuleCode.INVALID_COUPON_CODE,
        "O código deve conter exatamente 6 letras ou dígitos após a normalização.");
  }
}
