package com.maraujo.couponapi.auth.service;

public final class LoginRateLimitException extends RuntimeException {
  private final long retryAfterSeconds;

  public LoginRateLimitException(long retryAfterSeconds) {
    super("Muitas tentativas de autenticação. Aguarde antes de tentar novamente.");
    this.retryAfterSeconds = retryAfterSeconds;
  }

  public long retryAfterSeconds() {
    return retryAfterSeconds;
  }
}
