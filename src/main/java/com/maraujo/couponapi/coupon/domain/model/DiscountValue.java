package com.maraujo.couponapi.coupon.domain.model;

import com.maraujo.couponapi.coupon.domain.exception.BusinessRuleViolation;
import com.maraujo.couponapi.coupon.domain.exception.RuleCode;
import java.math.BigDecimal;

public record DiscountValue(BigDecimal value) {
  private static final BigDecimal MINIMUM = new BigDecimal("0.5");

  public DiscountValue {
    if (value == null || value.compareTo(MINIMUM) < 0) {
      throw new BusinessRuleViolation(
          RuleCode.INVALID_DISCOUNT_VALUE, "O desconto deve ser maior ou igual a 0,5.");
    }
    value = value.stripTrailingZeros();
  }
}
