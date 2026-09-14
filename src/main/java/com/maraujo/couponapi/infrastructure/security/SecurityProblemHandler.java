package com.maraujo.couponapi.infrastructure.security;

import com.maraujo.couponapi.shared.web.error.ProblemResponseWriter;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.*;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class SecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
  private final ProblemResponseWriter problems;

  public SecurityProblemHandler(ProblemResponseWriter problems) {
    this.problems = problems;
  }

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
      throws IOException {
    response.setHeader("WWW-Authenticate", "Bearer realm=\"coupon-api\"");
    problems.write(request, response, 401, "UNAUTHORIZED", "Autenticação ausente ou inválida.");
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
      throws IOException {
    response.setHeader("WWW-Authenticate", "Bearer error=\"insufficient_scope\"");
    problems.write(
        request, response, 403, "FORBIDDEN", "Permissão insuficiente para esta operação.");
  }
}
