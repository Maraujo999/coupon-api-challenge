package com.maraujo.couponapi.coupon.domain;

import static org.assertj.core.api.Assertions.*;

import com.maraujo.couponapi.coupon.domain.exception.*;
import com.maraujo.couponapi.coupon.domain.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

class CouponTest {
  static final Instant NOW = Instant.parse("2026-09-11T20:00:00Z");
  static final UUID ID = UUID.fromString("10000000-0000-4000-8000-000000000001");

  static Coupon create(Instant expiry, boolean published) {
    return Coupon.create(
        ID,
        "ABC-123",
        "  Desconto de lançamento  ",
        new BigDecimal("0.5001"),
        expiry,
        published,
        NOW);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void createsWithExpectedDefaultsAndPreservesData(boolean published) {
    var state = create(NOW.plusSeconds(60), published).snapshot();
    assertThat(state.id()).isEqualTo(ID);
    assertThat(state.code().value()).isEqualTo("ABC123");
    assertThat(state.description()).isEqualTo("  Desconto de lançamento  ");
    assertThat(state.discountValue().value()).isEqualByComparingTo("0.5001");
    assertThat(state.status()).isEqualTo(CouponStatus.ACTIVE);
    assertThat(state.published()).isEqualTo(published);
    assertThat(state.redeemed()).isFalse();
    assertThat(state.createdAt()).isEqualTo(NOW);
    assertThat(state.deletedAt()).isNull();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t", "\n"})
  void rejectsMissingDescription(String description) {
    assertThatThrownBy(
            () -> Coupon.create(ID, "ABC123", description, BigDecimal.ONE, NOW, false, NOW))
        .isInstanceOf(BusinessRuleViolation.class);
  }

  @Test
  void rejectsPastExpirationButAcceptsExactNow() {
    assertThatThrownBy(() -> create(NOW.minusNanos(1), false))
        .isInstanceOf(BusinessRuleViolation.class)
        .extracting("code")
        .isEqualTo(RuleCode.EXPIRATION_IN_PAST);
    assertThat(create(NOW, false).snapshot().expirationDate()).isEqualTo(NOW);
  }

  @Test
  void rejectsNullExpiration() {
    assertThatThrownBy(() -> create(null, false)).isInstanceOf(BusinessRuleViolation.class);
  }

  @Test
  void deletionPreservesRegistrationAndDoesNotMutateOriginal() {
    var original = create(NOW.plusSeconds(60), true);
    var deleted = original.delete(NOW.plusSeconds(120));
    assertThat(deleted.snapshot())
        .usingRecursiveComparison()
        .ignoringFields("status", "deletedAt")
        .isEqualTo(original.snapshot());
    assertThat(deleted.isDeleted()).isTrue();
    assertThat(original.isDeleted()).isFalse();
    assertThat(deleted.snapshot().deletedAt()).isEqualTo(NOW.plusSeconds(120));
    assertThatThrownBy(() -> deleted.delete(NOW.plusSeconds(180)))
        .isInstanceOf(BusinessRuleViolation.class)
        .extracting("code")
        .isEqualTo(RuleCode.COUPON_ALREADY_DELETED);
    assertThat(deleted.snapshot().deletedAt()).isEqualTo(NOW.plusSeconds(120));
  }

  @ParameterizedTest
  @EnumSource(
      value = CouponStatus.class,
      names = {"ACTIVE", "INACTIVE"})
  void canRestoreAndDeleteExpiredPublishedAndRedeemedCoupons(CouponStatus status) {
    var state =
        new CouponSnapshot(
            ID,
            new CouponCode("ABC123"),
            "Original",
            new DiscountValue(BigDecimal.ONE),
            NOW.minusSeconds(1),
            status,
            true,
            true,
            NOW.minusSeconds(100),
            null);
    var restored = Coupon.restore(state);
    assertThat(restored.snapshot()).isEqualTo(state);
    assertThat(restored.delete(NOW).isDeleted()).isTrue();
  }

  @Test
  void rejectsInconsistentDeletedStateAndDeletionBeforeCreation() {
    var active = create(NOW, false).snapshot();
    var inconsistent =
        new CouponSnapshot(
            ID,
            active.code(),
            active.description(),
            active.discountValue(),
            NOW,
            CouponStatus.DELETED,
            false,
            false,
            NOW,
            null);
    assertThatThrownBy(() -> Coupon.restore(inconsistent))
        .isInstanceOf(BusinessRuleViolation.class);
    var unexpectedDate =
        new CouponSnapshot(
            ID,
            active.code(),
            active.description(),
            active.discountValue(),
            NOW,
            CouponStatus.ACTIVE,
            false,
            false,
            NOW,
            NOW);
    assertThatThrownBy(() -> Coupon.restore(unexpectedDate))
        .isInstanceOf(BusinessRuleViolation.class);
    assertThatThrownBy(() -> create(NOW, false).delete(NOW.minusNanos(1)))
        .isInstanceOf(BusinessRuleViolation.class);
  }
}
