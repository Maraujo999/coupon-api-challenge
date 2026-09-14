package com.maraujo.couponapi.shared.web.filter;

import com.maraujo.couponapi.shared.web.error.ProblemResponseWriter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class JsonBodyLimitFilter extends OncePerRequestFilter {
  private static final int MAX_BYTES = 65536;
  private static final Set<String> BODY_METHODS = Set.of("POST", "PUT", "PATCH");
  private final ProblemResponseWriter problems;

  public JsonBodyLimitFilter(ProblemResponseWriter problems) {
    this.problems = problems;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !BODY_METHODS.contains(request.getMethod());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getContentLengthLong() > MAX_BYTES) {
      reject(request, response);
      return;
    }
    byte[] content = request.getInputStream().readNBytes(MAX_BYTES + 1);
    if (content.length > MAX_BYTES) {
      reject(request, response);
      return;
    }
    chain.doFilter(new BufferedRequest(request, content), response);
  }

  private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
    problems.write(
        request, response, 413, "PAYLOAD_TOO_LARGE", "O corpo da requisição excede 64 KiB.");
  }

  private static final class BufferedRequest extends HttpServletRequestWrapper {
    private final byte[] content;

    BufferedRequest(HttpServletRequest request, byte[] content) {
      super(request);
      this.content = content;
    }

    @Override
    public int getContentLength() {
      return content.length;
    }

    @Override
    public long getContentLengthLong() {
      return content.length;
    }

    @Override
    public BufferedReader getReader() {
      return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public ServletInputStream getInputStream() {
      var input = new ByteArrayInputStream(content);
      return new ServletInputStream() {
        @Override
        public int read() {
          return input.read();
        }

        @Override
        public int read(byte[] b, int off, int len) {
          return input.read(b, off, len);
        }

        @Override
        public boolean isFinished() {
          return input.available() == 0;
        }

        @Override
        public boolean isReady() {
          return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
          throw new UnsupportedOperationException(
              "Only synchronous request processing is supported.");
        }
      };
    }
  }
}
