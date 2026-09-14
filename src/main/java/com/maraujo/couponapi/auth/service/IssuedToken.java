package com.maraujo.couponapi.auth.service;

public record IssuedToken(String accessToken, String tokenType, long expiresIn) {
  @Override
  public String toString() {
    return "IssuedToken[token=REDACTED, expiresIn=" + expiresIn + "]";
  }
}
