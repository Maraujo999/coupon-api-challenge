package com.maraujo.couponapi.auth.web;

import com.maraujo.couponapi.auth.service.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/token")
public class TokenController {
  private final TokenService tokens;
  private final LoginAttemptLimiter limiter;

  public TokenController(TokenService tokens, LoginAttemptLimiter limiter) {
    this.tokens = tokens;
    this.limiter = limiter;
  }

  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Obter token da demonstração",
      description =
          "Credenciais configuradas no servidor. Permissões não são escolhidas pelo cliente.")
  public ResponseEntity<IssuedToken> issue(@Valid @RequestBody TokenRequest request) {
    limiter.check();
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header("Pragma", "no-cache")
        .body(tokens.issue(request.username(), request.password()));
  }
}
