package com.maraujo.couponapi.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "coupon.security.login-limit=1",
      "spring.datasource.url=jdbc:h2:mem:rate;DB_CLOSE_DELAY=-1"
    })
@ActiveProfiles("demo")
@AutoConfigureMockMvc
class TokenRateLimitIT {
  @Autowired MockMvc mvc;

  @Test
  void tokenEndpointLimitsAttemptsWithRetryHeaderAndProblemDetails() throws Exception {
    String login = "{\"username\":\"admin\",\"password\":\"wrong\"}";
    mvc.perform(post("/auth/token").contentType(MediaType.APPLICATION_JSON).content(login))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/auth/token").contentType(MediaType.APPLICATION_JSON).content(login))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"))
        .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
        .andExpect(jsonPath("$.correlationId").isString());
  }
}
