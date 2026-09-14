package com.maraujo.couponapi.config.security;

import jakarta.validation.constraints.*;
import java.time.Duration;
import org.springframework.boot.context.properties.*;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("coupon.security")
@Validated
public record SecurityProperties(
    @DefaultValue("admin") @NotBlank String username,
    @NotBlank @Size(min = 12, max = 72) String password,
    @DefaultValue("coupon-api") @NotBlank String issuer,
    @DefaultValue("coupon-api") @NotBlank String audience,
    @DefaultValue("PT15M") Duration tokenTtl,
    Resource privateKey,
    Resource publicKey,
    @DefaultValue("20") @Min(1) int loginLimit) {
  @Override
  public String toString() {
    return "SecurityProperties[credentials=REDACTED]";
  }
}
