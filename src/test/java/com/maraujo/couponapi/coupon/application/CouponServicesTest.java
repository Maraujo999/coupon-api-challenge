package com.maraujo.couponapi.coupon.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.maraujo.couponapi.coupon.application.command.CreateCouponCommand;
import com.maraujo.couponapi.coupon.application.exception.*;
import com.maraujo.couponapi.coupon.application.port.out.*;
import com.maraujo.couponapi.coupon.application.service.*;
import com.maraujo.couponapi.coupon.domain.exception.BusinessRuleViolation;
import com.maraujo.couponapi.coupon.domain.model.Coupon;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponServicesTest {
  static final Instant NOW = Instant.parse("2026-09-11T20:00:00Z");
  static final UUID ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
  static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
  @Mock CouponRepository repository;

  CreateCouponCommand command(String code) {
    return new CreateCouponCommand(
        code, "Original", new BigDecimal("0.5001"), NOW.plusSeconds(60), true);
  }

  Coupon coupon() {
    return Coupon.create(
        ID, "ABC123", "Original", BigDecimal.ONE, NOW, false, NOW.minusSeconds(60));
  }

  @Test
  void createsUsingDomainAndInjectedClockAndIdentifier() {
    var result = new CreateCouponService(repository, CLOCK, () -> ID).execute(command("ABC-123"));
    assertThat(result.snapshot().id()).isEqualTo(ID);
    assertThat(result.snapshot().code().value()).isEqualTo("ABC123");
    assertThat(result.snapshot().createdAt()).isEqualTo(NOW);
    verify(repository).insert(result);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void invalidDomainNeverWrites() {
    var service = new CreateCouponService(repository, CLOCK, () -> ID);
    assertThatThrownBy(() -> service.execute(command("ABC")))
        .isInstanceOf(BusinessRuleViolation.class);
    verifyNoInteractions(repository);
  }

  @Test
  void duplicateCodesDoNotInventUniquenessRequirement() {
    var service = new CreateCouponService(repository, CLOCK, UUID::randomUUID);
    var first = service.execute(command("ABC123"));
    var second = service.execute(command("ABC123"));
    assertThat(first.snapshot().id()).isNotEqualTo(second.snapshot().id());
    verify(repository, times(2)).insert(any());
  }

  @Test
  void failedCreationDoesNotReturnSuccess() {
    doThrow(new IllegalStateException("unavailable")).when(repository).insert(any());
    assertThatThrownBy(
            () -> new CreateCouponService(repository, CLOCK, () -> ID).execute(command("ABC123")))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void findsExistingEvenIfNowExpired() {
    var stored = coupon();
    when(repository.findById(ID)).thenReturn(Optional.of(new StoredCoupon(stored, 7)));
    assertThat(new GetCouponService(repository).execute(ID)).isSameAs(stored);
  }

  @Test
  void missingCouponIsNotFound() {
    when(repository.findById(ID)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> new GetCouponService(repository).execute(ID))
        .isInstanceOf(CouponNotFoundException.class);
  }

  @Test
  void deletedCouponIsNotFoundOnNormalRead() {
    when(repository.findById(ID))
        .thenReturn(Optional.of(new StoredCoupon(coupon().delete(NOW), 8)));
    assertThatThrownBy(() -> new GetCouponService(repository).execute(ID))
        .isInstanceOf(CouponNotFoundException.class);
  }

  @Test
  void deletesWithVersionOriginallyRead() {
    when(repository.findById(ID)).thenReturn(Optional.of(new StoredCoupon(coupon(), 7)));
    new DeleteCouponService(repository, CLOCK).execute(ID);
    var argument = ArgumentCaptor.forClass(Coupon.class);
    verify(repository).update(argument.capture(), eq(7L));
    assertThat(argument.getValue().isDeleted()).isTrue();
    assertThat(argument.getValue().snapshot().deletedAt()).isEqualTo(NOW);
  }

  @Test
  void missingDeletionDoesNotWrite() {
    when(repository.findById(ID)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> new DeleteCouponService(repository, CLOCK).execute(ID))
        .isInstanceOf(CouponNotFoundException.class);
    verify(repository, never()).update(any(), anyLong());
  }

  @Test
  void repeatedDeletionDoesNotWrite() {
    when(repository.findById(ID))
        .thenReturn(Optional.of(new StoredCoupon(coupon().delete(NOW), 8)));
    assertThatThrownBy(() -> new DeleteCouponService(repository, CLOCK).execute(ID))
        .isInstanceOf(BusinessRuleViolation.class);
    verify(repository, never()).update(any(), anyLong());
  }

  @Test
  void propagatesConcurrentModification() {
    when(repository.findById(ID)).thenReturn(Optional.of(new StoredCoupon(coupon(), 7)));
    doThrow(new ConcurrentCouponModificationException()).when(repository).update(any(), eq(7L));
    assertThatThrownBy(() -> new DeleteCouponService(repository, CLOCK).execute(ID))
        .isInstanceOf(ConcurrentCouponModificationException.class);
  }
}
