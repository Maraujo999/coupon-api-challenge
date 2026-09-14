package com.maraujo.couponapi.coupon.domain;

import static org.assertj.core.api.Assertions.*;

import com.maraujo.couponapi.coupon.domain.exception.BusinessRuleViolation;
import com.maraujo.couponapi.coupon.domain.model.DiscountValue;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DiscountValueTest {
  @ParameterizedTest
  @ValueSource(
      strings = {
        "0.5",
        "0.50",
        "0.5001",
        "101",
        "99999999999999999999999999999999999999.123456789"
      })
  void acceptsExactValuesWithoutPercentageCeiling(String input) {
    assertThat(new DiscountValue(new BigDecimal(input)).value()).isEqualByComparingTo(input);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.4999", "0", "-1"})
  void rejectsValuesBelowMinimum(String input) {
    assertThatThrownBy(() -> new DiscountValue(new BigDecimal(input)))
        .isInstanceOf(BusinessRuleViolation.class);
  }

  @Test
  void rejectsNull() {
    assertThatThrownBy(() -> new DiscountValue(null)).isInstanceOf(BusinessRuleViolation.class);
  }

  @Test
  void equalityDoesNotDependOnDecimalScale() {
    assertThat(new DiscountValue(new BigDecimal("0.50")))
        .isEqualTo(new DiscountValue(new BigDecimal("0.5")));
  }
}
