package com.maraujo.couponapi.shared.web.error;

import com.maraujo.couponapi.auth.service.LoginRateLimitException;
import com.maraujo.couponapi.coupon.application.exception.*;
import com.maraujo.couponapi.coupon.domain.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Comparator;
import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private final ProblemDetailFactory problems;

  public GlobalExceptionHandler(ProblemDetailFactory problems) {
    this.problems = problems;
  }

  @ExceptionHandler(BadCredentialsException.class)
  ResponseEntity<ProblemDetail> badCredentials(HttpServletRequest request) {
    return ResponseEntity.status(401)
        .header("WWW-Authenticate", "Bearer realm=\"coupon-api\"")
        .body(problems.create(401, "UNAUTHORIZED", "Autenticação ausente ou inválida.", request));
  }

  @ExceptionHandler(LoginRateLimitException.class)
  ResponseEntity<ProblemDetail> rateLimit(LoginRateLimitException ex, HttpServletRequest request) {
    return ResponseEntity.status(429)
        .header("Retry-After", Long.toString(ex.retryAfterSeconds()))
        .body(problems.create(429, "TOO_MANY_REQUESTS", ex.getMessage(), request));
  }

  @ExceptionHandler(BusinessRuleViolation.class)
  ResponseEntity<ProblemDetail> business(BusinessRuleViolation ex, HttpServletRequest request) {
    int status = ex.code() == RuleCode.COUPON_ALREADY_DELETED ? 409 : 422;
    return response(status, ex.code().name(), ex.getMessage(), request);
  }

  @ExceptionHandler(CouponNotFoundException.class)
  ResponseEntity<ProblemDetail> notFound(CouponNotFoundException ex, HttpServletRequest request) {
    return response(404, "COUPON_NOT_FOUND", ex.getMessage(), request);
  }

  @ExceptionHandler(ConcurrentCouponModificationException.class)
  ResponseEntity<ProblemDetail> concurrent(
      ConcurrentCouponModificationException ex, HttpServletRequest request) {
    return response(409, "COUPON_CONCURRENT_MODIFICATION", ex.getMessage(), request);
  }

  @ExceptionHandler(NumericRepresentationException.class)
  ResponseEntity<ProblemDetail> numeric(
      NumericRepresentationException ex, HttpServletRequest request) {
    return response(422, "NUMBER_OUT_OF_RANGE", ex.getMessage(), request);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetail> unexpected(Exception ex, HttpServletRequest request) {
    LOG.error("Unexpected request failure", ex);
    return response(500, "INTERNAL_ERROR", "Não foi possível concluir a operação.", request);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    var problem =
        problems.create(400, "INVALID_REQUEST", "Revise os campos obrigatórios.", servlet(request));
    problem.setProperty(
        "errors",
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldViolation(error.getField(), "REQUIRED", "Campo obrigatório."))
            .distinct()
            .sorted(Comparator.comparing(FieldViolation::field))
            .toList());
    return new ResponseEntity<>(problem, headers, status);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    String code =
        switch (status.value()) {
          case 404 -> "NOT_FOUND";
          case 405 -> "METHOD_NOT_ALLOWED";
          case 406 -> "NOT_ACCEPTABLE";
          case 415 -> "UNSUPPORTED_MEDIA_TYPE";
          default -> "INVALID_REQUEST";
        };
    return new ResponseEntity<>(
        problems.create(
            status.value(),
            code,
            "A requisição não é compatível com o contrato da API.",
            servlet(request)),
        headers,
        status);
  }

  private ResponseEntity<ProblemDetail> response(
      int status, String code, String detail, HttpServletRequest request) {
    return ResponseEntity.status(status).body(problems.create(status, code, detail, request));
  }

  private static HttpServletRequest servlet(WebRequest request) {
    return ((ServletWebRequest) request).getRequest();
  }
}
