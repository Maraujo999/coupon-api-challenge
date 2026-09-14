package com.maraujo.couponapi.config.security;

import static org.assertj.core.api.Assertions.*;

import com.maraujo.couponapi.auth.web.TokenRequest;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SecurityConfigurationTest {
  ApplicationContextRunner runner() {
    return new ApplicationContextRunner()
        .withUserConfiguration(JwtConfiguration.class)
        .withBean(Clock.class, Clock::systemUTC)
        .withPropertyValues("coupon.security.password=only-integration-fixture");
  }

  @Test
  void outsideDemoRequiresExplicitKeys() {
    runner()
        .run(
            context ->
                assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasStackTraceContaining("Configure RSA private/public keys"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"PT0S", "PT2H"})
  void invalidTtlCannotStart(String ttl) {
    runner()
        .withInitializer(context -> context.getEnvironment().setActiveProfiles("demo"))
        .withPropertyValues("coupon.security.token-ttl=" + ttl)
        .run(
            context ->
                assertThat(context.getStartupFailure()).hasStackTraceContaining("Token TTL"));
  }

  @Test
  void rejectsBcryptPasswordLongerThan72Utf8BytesAndMissingPassword() {
    runner()
        .withInitializer(context -> context.getEnvironment().setActiveProfiles("demo"))
        .withPropertyValues("coupon.security.password=" + "á".repeat(37))
        .run(context -> assertThat(context.getStartupFailure()).hasStackTraceContaining("72-byte"));
    runner()
        .withPropertyValues("coupon.security.password=")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void credentialRecordsCannotLeakViaToString() {
    assertThat(new TokenRequest("admin", "sensitive").toString()).doesNotContain("sensitive");
    var properties =
        new SecurityProperties(
            "admin",
            "sensitive",
            "issuer",
            "audience",
            java.time.Duration.ofMinutes(15),
            null,
            null,
            20);
    assertThat(properties.toString()).doesNotContain("sensitive");
  }
}
