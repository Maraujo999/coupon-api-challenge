package com.maraujo.couponapi.config.security;

import com.maraujo.couponapi.auth.service.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.env.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SecurityProperties.class)
public class JwtConfiguration {
  @Bean
  RSAKey rsaKey(SecurityProperties properties, Environment environment) throws Exception {
    return environment.acceptsProfiles(Profiles.of("demo"))
        ? RsaKeyProvider.generate()
        : RsaKeyProvider.load(properties.privateKey(), properties.publicKey());
  }

  @Bean
  JwtEncoder jwtEncoder(RSAKey key) {
    return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
  }

  @Bean
  JwtDecoder jwtDecoder(RSAKey key, SecurityProperties properties, Clock clock) throws Exception {
    var decoder =
        NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey())
            .signatureAlgorithm(SignatureAlgorithm.RS256)
            .build();
    var time = new JwtTimestampValidator(Duration.ofSeconds(30));
    time.setClock(clock);
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            time,
            new JwtIssuerValidator(properties.issuer()),
            new JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD, value -> value != null && value.contains(properties.audience())),
            new JwtClaimValidator<Instant>(JwtClaimNames.EXP, value -> value != null)));
    return decoder;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  AuthenticationManager authenticationManager(
      SecurityProperties properties, PasswordEncoder passwords) {
    if (properties.password().getBytes(StandardCharsets.UTF_8).length > 72) {
      throw new IllegalStateException("Password exceeds BCrypt's 72-byte limit.");
    }
    var user =
        User.withUsername(properties.username())
            .password(passwords.encode(properties.password()))
            .authorities("SCOPE_coupon:read", "SCOPE_coupon:write")
            .build();
    var provider = new DaoAuthenticationProvider(new InMemoryUserDetailsManager(user));
    provider.setPasswordEncoder(passwords);
    return new ProviderManager(provider);
  }

  @Bean
  TokenService tokenService(
      AuthenticationManager authentication,
      JwtEncoder encoder,
      Clock clock,
      SecurityProperties properties) {
    if (properties.tokenTtl().compareTo(Duration.ofSeconds(1)) < 0
        || properties.tokenTtl().compareTo(Duration.ofHours(1)) > 0) {
      throw new IllegalStateException("Token TTL must be between one second and one hour.");
    }
    return new TokenService(
        authentication,
        encoder,
        clock,
        properties.issuer(),
        properties.audience(),
        properties.tokenTtl());
  }

  @Bean
  LoginAttemptLimiter loginAttemptLimiter(Clock clock, SecurityProperties properties) {
    return new LoginAttemptLimiter(clock, properties.loginLimit(), Duration.ofMinutes(1));
  }
}
