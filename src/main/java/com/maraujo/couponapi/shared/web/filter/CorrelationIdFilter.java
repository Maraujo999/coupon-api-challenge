package com.maraujo.couponapi.shared.web.filter;

import com.maraujo.couponapi.shared.web.error.ProblemDetailFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
  private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");
  private static final Logger LOG = LoggerFactory.getLogger(CorrelationIdFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var received = request.getHeader("X-Correlation-ID");
    var id =
        received != null && VALID_ID.matcher(received).matches()
            ? received
            : UUID.randomUUID().toString();
    request.setAttribute(ProblemDetailFactory.CORRELATION_ATTRIBUTE, id);
    response.setHeader("X-Correlation-ID", id);
    MDC.put("correlationId", id);
    long started = System.nanoTime();
    boolean completed = false;
    try {
      chain.doFilter(request, response);
      completed = true;
    } finally {
      Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
      LOG.info(
          "http_request method={} route={} status={} durationMs={}",
          request.getMethod(),
          route == null ? "unmatched" : route,
          completed ? response.getStatus() : 500,
          (System.nanoTime() - started) / 1_000_000);
      MDC.remove("correlationId");
    }
  }
}
