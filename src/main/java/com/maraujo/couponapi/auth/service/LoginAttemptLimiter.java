package com.maraujo.couponapi.auth.service;

import java.time.*;

/** Bounded, instance-wide fixed window for the self-contained demonstration issuer. */
public final class LoginAttemptLimiter {
  private final Clock clock;
  private final int limit;
  private final Duration window;
  private Instant started;
  private int attempts;

  public LoginAttemptLimiter(Clock clock, int limit, Duration window) {
    if (limit < 1 || window.compareTo(Duration.ofSeconds(1)) < 0) {
      throw new IllegalArgumentException("Login limit and window must be positive.");
    }
    this.clock = clock;
    this.limit = limit;
    this.window = window;
    this.started = clock.instant();
  }

  public synchronized void check() {
    var now = clock.instant();
    if (!now.isBefore(started.plus(window))) {
      started = now;
      attempts = 0;
    }
    if (attempts >= limit) {
      long millis = Duration.between(now, started.plus(window)).toMillis();
      throw new LoginRateLimitException(Math.max(1, (millis + 999) / 1000));
    }
    attempts++;
  }
}
