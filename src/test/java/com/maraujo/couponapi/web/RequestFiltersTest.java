package com.maraujo.couponapi.web;

import static org.assertj.core.api.Assertions.*;

import com.maraujo.couponapi.shared.web.error.*;
import com.maraujo.couponapi.shared.web.filter.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.*;
import org.slf4j.MDC;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.web.*;
import tools.jackson.databind.json.JsonMapper;

class RequestFiltersTest {
  final CorrelationIdFilter correlation = new CorrelationIdFilter();
  final ProblemResponseWriter writer =
      new ProblemResponseWriter(
          new ProblemDetailFactory(),
          (JsonMapper) new JacksonJsonHttpMessageConverter().getMapper());
  final JsonBodyLimitFilter limits = new JsonBodyLimitFilter(writer);

  @AfterEach
  void cleanContext() {
    MDC.clear();
  }

  @Test
  void generatesAndPropagatesCorrelationAndAlwaysClearsMdc() throws Exception {
    var request = new MockHttpServletRequest("GET", "/coupon/id");
    var response = new MockHttpServletResponse();
    correlation.doFilter(
        request,
        response,
        (r, s) -> {
          assertThat(MDC.get("correlationId")).isEqualTo(response.getHeader("X-Correlation-ID"));
          assertThat(MDC.get("correlationId")).isNotBlank();
        });
    assertThat(MDC.get("correlationId")).isNull();
    request.addHeader("X-Correlation-ID", "test-123");
    correlation.doFilter(
        request, response, (r, s) -> assertThat(MDC.get("correlationId")).isEqualTo("test-123"));
    assertThat(MDC.get("correlationId")).isNull();
  }

  @Test
  void invalidHeaderCannotInjectLogsAndFailureClearsMdc() {
    var request = new MockHttpServletRequest("GET", "/coupon");
    request.addHeader("X-Correlation-ID", "bad\r\nheader");
    var response = new MockHttpServletResponse();
    assertThatThrownBy(
            () ->
                correlation.doFilter(
                    request,
                    response,
                    (r, s) -> {
                      throw new IOException("failure");
                    }))
        .isInstanceOf(IOException.class);
    assertThat(response.getHeader("X-Correlation-ID")).matches("[a-f0-9-]{36}");
    assertThat(MDC.get("correlationId")).isNull();
  }

  @Test
  void rejectsOversizedBodyEvenWithoutContentLength() throws Exception {
    var request =
        new MockHttpServletRequest("POST", "/coupon") {
          @Override
          public int getContentLength() {
            return -1;
          }

          @Override
          public long getContentLengthLong() {
            return -1;
          }
        };
    request.setContentType("application/json");
    request.setContent(new byte[65537]);
    var response = new MockHttpServletResponse();
    limits.doFilter(request, response, (r, s) -> fail("Oversized request reached application"));
    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(response.getContentAsString()).contains("PAYLOAD_TOO_LARGE");
  }

  @Test
  void acceptsBoundaryWithoutLosingBodyAndRejectsKnownLength() throws Exception {
    var request = new MockHttpServletRequest("POST", "/coupon");
    request.setContent(new byte[65536]);
    var response = new MockHttpServletResponse();
    limits.doFilter(
        request, response, (r, s) -> assertThat(r.getInputStream().readAllBytes()).hasSize(65536));
    request.setContent(new byte[65537]);
    limits.doFilter(request, response, (r, s) -> fail("Oversized request reached application"));
    assertThat(response.getStatus()).isEqualTo(413);
  }

  @Test
  void preservesUtf8ReaderAndPassesRequestsWithoutBody() throws Exception {
    var request = new MockHttpServletRequest("POST", "/auth/token");
    request.setContent("ação".getBytes(StandardCharsets.UTF_8));
    limits.doFilter(
        request,
        new MockHttpServletResponse(),
        (r, s) -> assertThat(r.getReader().readLine()).isEqualTo("ação"));
    limits.doFilter(
        new MockHttpServletRequest("GET", "/actuator/health"),
        new MockHttpServletResponse(),
        (r, s) -> assertThat(r.getContentLength()).isEqualTo(-1));
  }
}
