package com.maraujo.couponapi.auth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.maraujo.couponapi.auth.service.TokenService;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {
  @Mock AuthenticationManager authentication;
  @Mock JwtEncoder encoder;
  final Instant now = Instant.parse("2026-09-11T20:00:00Z");

  TokenService service() {
    return new TokenService(
        authentication,
        encoder,
        Clock.fixed(now, ZoneOffset.UTC),
        "issuer",
        "audience",
        Duration.ofMinutes(15));
  }

  @Test
  void signsClaimsWithServerPermissionsAndFixedClock() {
    when(authentication.authenticate(any()))
        .thenReturn(
            UsernamePasswordAuthenticationToken.authenticated(
                "admin",
                null,
                List.of(
                    new SimpleGrantedAuthority("SCOPE_coupon:read"),
                    new SimpleGrantedAuthority("SCOPE_coupon:write"))));
    when(encoder.encode(any()))
        .thenAnswer(
            inv -> {
              JwtEncoderParameters parameters = inv.getArgument(0);
              assertThat(parameters.getJwsHeader().getAlgorithm().getName()).isEqualTo("RS256");
              assertThat(parameters.getClaims().getClaimAsString("iss")).isEqualTo("issuer");
              assertThat(parameters.getClaims().getAudience()).containsExactly("audience");
              assertThat(parameters.getClaims().getSubject()).isEqualTo("admin");
              assertThat(parameters.getClaims().getExpiresAt()).isEqualTo(now.plusSeconds(900));
              assertThat(parameters.getClaims().getClaimAsString("scope"))
                  .isEqualTo("coupon:read coupon:write");
              return new Jwt(
                  "encoded-token",
                  now,
                  now.plusSeconds(900),
                  java.util.Map.of("alg", "RS256"),
                  parameters.getClaims().getClaims());
            });
    var issued = service().issue("admin", "test-password");
    assertThat(issued.accessToken()).isEqualTo("encoded-token");
    assertThat(issued.tokenType()).isEqualTo("Bearer");
    assertThat(issued.expiresIn()).isEqualTo(900);
    assertThat(issued.toString()).doesNotContain("encoded-token");
  }

  @Test
  void badCredentialsNeverReachEncoder() {
    when(authentication.authenticate(any()))
        .thenThrow(new BadCredentialsException("Bad credentials"));
    assertThatThrownBy(() -> service().issue("admin", "wrong"))
        .isInstanceOf(BadCredentialsException.class);
    verifyNoInteractions(encoder);
  }

  @Test
  void bcryptByteLimitDoesNotBecome500() {
    assertThatThrownBy(() -> service().issue("admin", "á".repeat(37)))
        .isInstanceOf(BadCredentialsException.class);
    verifyNoInteractions(authentication, encoder);
  }
}
