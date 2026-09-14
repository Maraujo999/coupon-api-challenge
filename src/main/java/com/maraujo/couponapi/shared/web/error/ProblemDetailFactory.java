package com.maraujo.couponapi.shared.web.error;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.*;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailFactory {
  public static final String CORRELATION_ATTRIBUTE = "coupon.correlationId";

  public ProblemDetail create(int status, String code, String detail, HttpServletRequest request) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
    problem.setType(
        URI.create("urn:coupon-api:error:" + code.toLowerCase(Locale.ROOT).replace('_', '-')));
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("code", code);
    var correlation = request.getAttribute(CORRELATION_ATTRIBUTE);
    problem.setProperty(
        "correlationId", correlation == null ? UUID.randomUUID().toString() : correlation);
    return problem;
  }
}
