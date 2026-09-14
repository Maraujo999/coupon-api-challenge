package com.maraujo.couponapi.transaction;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.maraujo.couponapi.coupon.application.command.CreateCouponCommand;
import com.maraujo.couponapi.coupon.application.port.in.*;
import com.maraujo.couponapi.coupon.domain.model.Coupon;
import com.maraujo.couponapi.infrastructure.transaction.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;

@ExtendWith(MockitoExtension.class)
class TransactionBoundaryTest {
  @Mock PlatformTransactionManager manager;
  @Mock CreateCouponUseCase create;
  @Mock DeleteCouponUseCase delete;
  final TransactionStatus status = new SimpleTransactionStatus();
  final CreateCouponCommand command =
      new CreateCouponCommand("ABC123", "Test", BigDecimal.ONE, Instant.MAX, false);

  @BeforeEach
  void transaction() {
    when(manager.getTransaction(any())).thenReturn(status);
  }

  @Test
  void returnsCreationOnlyAfterCommit() {
    var coupon =
        Coupon.create(
            UUID.randomUUID(), "ABC123", "Test", BigDecimal.ONE, Instant.MAX, false, Instant.EPOCH);
    when(create.execute(command)).thenReturn(coupon);
    assertThat(
            new TransactionalCreateCoupon(create, new TransactionTemplate(manager))
                .execute(command))
        .isSameAs(coupon);
    var order = inOrder(manager, create);
    order.verify(manager).getTransaction(any());
    order.verify(create).execute(command);
    order.verify(manager).commit(status);
  }

  @Test
  void commitFailureNeverReturnsSuccess() {
    when(create.execute(command))
        .thenReturn(
            Coupon.create(
                UUID.randomUUID(),
                "ABC123",
                "Test",
                BigDecimal.ONE,
                Instant.MAX,
                false,
                Instant.EPOCH));
    doThrow(new TransactionSystemException("commit failed")).when(manager).commit(status);
    assertThatThrownBy(
            () ->
                new TransactionalCreateCoupon(create, new TransactionTemplate(manager))
                    .execute(command))
        .isInstanceOf(TransactionSystemException.class);
  }

  @Test
  void domainFailureRollsBackDeletion() {
    var id = UUID.randomUUID();
    doThrow(new IllegalStateException("failure")).when(delete).execute(id);
    assertThatThrownBy(
            () ->
                new TransactionalDeleteCoupon(delete, new TransactionTemplate(manager)).execute(id))
        .isInstanceOf(IllegalStateException.class);
    verify(manager).rollback(status);
    verify(manager, never()).commit(any());
  }

  @Test
  void deletionCommitsBeforeReturning() {
    var id = UUID.randomUUID();
    new TransactionalDeleteCoupon(delete, new TransactionTemplate(manager)).execute(id);
    var order = inOrder(manager, delete);
    order.verify(manager).getTransaction(any());
    order.verify(delete).execute(id);
    order.verify(manager).commit(status);
  }
}
