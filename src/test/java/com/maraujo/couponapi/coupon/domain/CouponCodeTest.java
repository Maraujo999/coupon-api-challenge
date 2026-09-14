package com.maraujo.couponapi.coupon.domain;

import static org.assertj.core.api.Assertions.*;

import com.maraujo.couponapi.coupon.domain.exception.BusinessRuleViolation;
import com.maraujo.couponapi.coupon.domain.model.CouponCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CouponCodeTest {
  @ParameterizedTest
  @ValueSource(strings = {"ABC123", "ABC-123", " A!B@C#1$2%3 ", "áABC123🙂"})
  void normalizesBeforeCheckingLength(String input) {
    assertThat(new CouponCode(input).value()).isEqualTo("ABC123");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"ABC12", "ABC1234", "---", "      ", "éáàçü1"})
  void rejectsCodesWithoutExactlySixAsciiLettersOrDigits(String input) {
    assertThatThrownBy(() -> new CouponCode(input)).isInstanceOf(BusinessRuleViolation.class);
  }

  @Test
  void preservesCaseAndNormalizationIsIdempotent() {
    var code = new CouponCode("abc-123");
    assertThat(code.value()).isEqualTo("abc123");
    assertThat(new CouponCode(code.value())).isEqualTo(code);
  }
}
