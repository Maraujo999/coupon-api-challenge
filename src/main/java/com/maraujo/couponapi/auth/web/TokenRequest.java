package com.maraujo.couponapi.auth.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record TokenRequest(
    @NotBlank @Size(max = 100) String username,
    @NotBlank @Size(max = 72) @Schema(format = "password") String password) {
  @Override
  public String toString() {
    return "TokenRequest[credentials=REDACTED]";
  }
}
