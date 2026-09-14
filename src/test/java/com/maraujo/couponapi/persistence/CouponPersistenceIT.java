package com.maraujo.couponapi.persistence;

import static org.assertj.core.api.Assertions.*;

import com.maraujo.couponapi.coupon.application.exception.ConcurrentCouponModificationException;
import com.maraujo.couponapi.coupon.application.port.out.CouponRepository;
import com.maraujo.couponapi.coupon.domain.model.Coupon;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {"spring.datasource.url=jdbc:h2:mem:persistence;DB_CLOSE_DELAY=-1"})
@ActiveProfiles("demo")
class CouponPersistenceIT {
  static final Instant NOW = Instant.parse("2026-09-11T20:00:00.123456789Z");
  @Autowired CouponRepository repository;
  @Autowired PlatformTransactionManager manager;
  @Autowired JdbcTemplate jdbc;
  @Autowired Flyway flyway;
  TransactionTemplate tx;

  @BeforeEach
  void transactions() {
    tx = new TransactionTemplate(manager);
  }

  Coupon coupon(String amount) {
    return Coupon.create(
        UUID.randomUUID(),
        "ABC-123",
        "Descrição longa: " + "a".repeat(500),
        new BigDecimal(amount),
        NOW.plusSeconds(60),
        true,
        NOW);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"0.5", "0.5001", "100000000000000000000000000000000000000.123456789", "1E+1000"})
  void roundTripsExactDecimalAndNanosecondInstant(String amount) {
    var original = coupon(amount);
    tx.executeWithoutResult(s -> repository.insert(original));
    var restored = tx.execute(s -> repository.findById(original.snapshot().id()).orElseThrow());
    assertThat(restored.coupon().snapshot())
        .usingRecursiveComparison()
        .isEqualTo(original.snapshot());
    assertThat(restored.version()).isZero();
  }

  @Test
  void migrateIsRepeatableWithoutDestroyingData() {
    var original = coupon("1");
    tx.executeWithoutResult(s -> repository.insert(original));
    assertThat(flyway.info().current()).isNotNull();
    assertThat(flyway.migrate().migrationsExecuted).isZero();
    assertThat(repository.findById(original.snapshot().id())).isPresent();
  }

  @Test
  void softDeletePreservesRegistrationAndVersionAndAllowsExpiredRead() {
    var original = coupon("0.5001");
    var id = original.snapshot().id();
    tx.executeWithoutResult(s -> repository.insert(original));
    tx.executeWithoutResult(
        s -> {
          var stored = repository.findById(id).orElseThrow();
          repository.update(stored.coupon().delete(NOW.plusSeconds(3600)), stored.version());
        });
    var stored = repository.findById(id).orElseThrow();
    assertThat(stored.coupon().isDeleted()).isTrue();
    assertThat(stored.version()).isEqualTo(1);
    assertThat(stored.coupon().snapshot())
        .usingRecursiveComparison()
        .ignoringFields("deletedAt", "status")
        .isEqualTo(original.snapshot());
    assertThat(jdbc.queryForObject("select count(*) from coupon where id = ?", Integer.class, id))
        .isEqualTo(1);
  }

  @Test
  void failedTransactionRollsBackInsert() {
    var original = coupon("1");
    assertThatThrownBy(
            () ->
                tx.executeWithoutResult(
                    s -> {
                      repository.insert(original);
                      throw new IllegalStateException("force rollback");
                    }))
        .isInstanceOf(IllegalStateException.class);
    assertThat(repository.findById(original.snapshot().id())).isEmpty();
  }

  @Test
  void duplicateCodesCanCoexistAndMissingIdIsEmpty() {
    var first = coupon("1");
    var second = coupon("2");
    tx.executeWithoutResult(
        s -> {
          repository.insert(first);
          repository.insert(second);
        });
    assertThat(repository.findById(first.snapshot().id())).isPresent();
    assertThat(repository.findById(second.snapshot().id())).isPresent();
    assertThat(repository.findById(UUID.randomUUID())).isEmpty();
  }

  @Test
  void staleVersionIsRejectedWithoutChangingDeletionDate() {
    var original = coupon("1");
    tx.executeWithoutResult(s -> repository.insert(original));
    tx.executeWithoutResult(s -> repository.update(original.delete(NOW), 0));
    assertThatThrownBy(
            () ->
                tx.executeWithoutResult(
                    s -> repository.update(original.delete(NOW.plusSeconds(1)), 0)))
        .isInstanceOf(ConcurrentCouponModificationException.class);
    assertThat(
            repository
                .findById(original.snapshot().id())
                .orElseThrow()
                .coupon()
                .snapshot()
                .deletedAt())
        .isEqualTo(NOW);
  }

  @Test
  @Timeout(20)
  void simultaneousTransactionsConfirmOnlyOneDeletion() throws Exception {
    var original = coupon("1");
    tx.executeWithoutResult(s -> repository.insert(original));
    var barrier = new CyclicBarrier(2);
    Callable<Boolean> deletion =
        () -> {
          try {
            new TransactionTemplate(manager)
                .executeWithoutResult(
                    s -> {
                      var stored = repository.findById(original.snapshot().id()).orElseThrow();
                      try {
                        barrier.await(5, TimeUnit.SECONDS);
                      } catch (Exception e) {
                        throw new IllegalStateException(e);
                      }
                      repository.update(
                          stored.coupon().delete(NOW.plusSeconds(1)), stored.version());
                    });
            return true;
          } catch (ConcurrentCouponModificationException e) {
            return false;
          }
        };
    try (var pool = Executors.newFixedThreadPool(2)) {
      var first = pool.submit(deletion);
      var second = pool.submit(deletion);
      assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(true, false);
    }
    var finalState = repository.findById(original.snapshot().id()).orElseThrow();
    assertThat(finalState.version()).isEqualTo(1);
    assertThat(finalState.coupon().isDeleted()).isTrue();
  }

  @Test
  void databaseRejectsInvalidStaticState() {
    var original = coupon("1");
    tx.executeWithoutResult(s -> repository.insert(original));
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "update coupon set status = 'DELETED' where id = ?", original.snapshot().id()))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "update coupon set discount_value = 0.49 where id = ?",
                    original.snapshot().id()))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  }
}
