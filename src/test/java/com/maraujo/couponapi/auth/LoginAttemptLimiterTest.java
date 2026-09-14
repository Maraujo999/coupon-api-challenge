package com.maraujo.couponapi.auth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.maraujo.couponapi.auth.service.*;
import java.time.*;
import org.junit.jupiter.api.Test;

class LoginAttemptLimiterTest {
  @Test
  void limitAndWindowBoundaryUseInjectedClock() {
    var clock = mock(Clock.class);
    var now = Instant.parse("2026-09-11T20:00:00Z");
    when(clock.instant()).thenReturn(now);
    var limiter = new LoginAttemptLimiter(clock, 2, Duration.ofMinutes(1));
    limiter.check();
    limiter.check();
    assertThatThrownBy(limiter::check)
        .isInstanceOf(LoginRateLimitException.class)
        .extracting("retryAfterSeconds")
        .isEqualTo(60L);
    when(clock.instant()).thenReturn(now.plusSeconds(60));
    assertThatCode(limiter::check).doesNotThrowAnyException();
  }

  @Test
  void retryAfterRoundsUpAndInvalidConfigurationFails() {
    var clock = mock(Clock.class);
    var now = Instant.parse("2026-09-11T20:00:00Z");
    when(clock.instant()).thenReturn(now);
    var limiter = new LoginAttemptLimiter(clock, 1, Duration.ofSeconds(1));
    limiter.check();
    when(clock.instant()).thenReturn(now.plusMillis(500));
    assertThatThrownBy(limiter::check).extracting("retryAfterSeconds").isEqualTo(1L);
    assertThatThrownBy(() -> new LoginAttemptLimiter(clock, 0, Duration.ofMinutes(1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new LoginAttemptLimiter(clock, 1, Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
