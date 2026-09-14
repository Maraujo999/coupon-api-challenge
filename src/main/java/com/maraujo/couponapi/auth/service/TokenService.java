package com.maraujo.couponapi.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.authentication.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

public final class TokenService {
  private final AuthenticationManager authentication;
  private final JwtEncoder encoder;
  private final Clock clock;
  private final String issuer;
  private final String audience;
  private final Duration ttl;

  public TokenService(
      AuthenticationManager authentication,
      JwtEncoder encoder,
      Clock clock,
      String issuer,
      String audience,
      Duration ttl) {
    this.authentication = authentication;
    this.encoder = encoder;
    this.clock = clock;
    this.issuer = issuer;
    this.audience = audience;
    this.ttl = ttl;
  }

  public IssuedToken issue(String username, String password) {
    if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
      throw new BadCredentialsException("Invalid credentials");
    }
    var authenticated =
        authentication.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(username, password));
    var scopes =
        authenticated.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .filter(value -> value.startsWith("SCOPE_"))
            .map(value -> value.substring(6))
            .sorted()
            .collect(Collectors.joining(" "));
    var now = clock.instant();
    var claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .subject(authenticated.getName())
            .audience(List.of(audience))
            .issuedAt(now)
            .expiresAt(now.plus(ttl))
            .claim("scope", scopes)
            .build();
    var headers = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();
    var token = encoder.encode(JwtEncoderParameters.from(headers, claims));
    return new IssuedToken(token.getTokenValue(), "Bearer", ttl.toSeconds());
  }
}
