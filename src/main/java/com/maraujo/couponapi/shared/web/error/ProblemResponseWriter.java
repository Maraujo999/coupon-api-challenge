package com.maraujo.couponapi.shared.web.error;

import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ProblemResponseWriter {
  private final ProblemDetailFactory problems;
  private final JsonMapper mapper;

  public ProblemResponseWriter(ProblemDetailFactory problems, JsonMapper mapper) {
    this.problems = problems;
    this.mapper = mapper;
  }

  public void write(
      HttpServletRequest request,
      HttpServletResponse response,
      int status,
      String code,
      String detail)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    mapper.writeValue(response.getOutputStream(), problems.create(status, code, detail, request));
  }
}
