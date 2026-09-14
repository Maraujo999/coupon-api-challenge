package com.maraujo.couponapi.auth;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
    properties = {
      "coupon.security.password=only-integration-fixture",
      "coupon.security.login-limit=200",
      "spring.datasource.url=jdbc:h2:mem:security;DB_CLOSE_DELAY=-1"
    })
@ActiveProfiles("demo")
@AutoConfigureMockMvc
class ApiSecurityIT {
  @Autowired MockMvc mvc;
  @Autowired JsonMapper json;
  @Autowired JwtEncoder encoder;

  String body() {
    return "{\"code\":\"ABC-123\",\"description\":\"Smoke\",\"discountValue\":0.5001,\"expirationDate\":\""
        + Instant.now().plusSeconds(86400)
        + "\"}";
  }

  String signed(String variation, String scope) {
    var now = Instant.now();
    var claims =
        JwtClaimsSet.builder()
            .issuer(variation.equals("issuer") ? "wrong" : "coupon-api")
            .subject("tester")
            .audience(List.of(variation.equals("audience") ? "wrong" : "coupon-api"))
            .issuedAt(now.minusSeconds(600))
            .claim("scope", scope);
    if (!variation.equals("missing-exp")) {
      claims.expiresAt(variation.equals("expired") ? now.minusSeconds(300) : now.plusSeconds(300));
    }
    if (variation.equals("future")) {
      claims.notBefore(now.plusSeconds(120));
    }
    return encoder
        .encode(
            JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(), claims.build()))
        .getTokenValue();
  }

  @Test
  void missingTokenHasUniformProblemAndCorrelation() throws Exception {
    mvc.perform(get("/coupon/" + UUID.randomUUID()).header("X-Correlation-ID", "security-test"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().exists("WWW-Authenticate"))
        .andExpect(header().string("X-Correlation-ID", "security-test"))
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.correlationId").value("security-test"));
  }

  @Test
  void loginAndCompleteCouponLifecycleUseRealSignedJwt() throws Exception {
    var login =
        mvc.perform(
                post("/auth/token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"admin\",\"password\":\"only-integration-fixture\"}"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andReturn();
    var token =
        json.readTree(login.getResponse().getContentAsString()).get("accessToken").asString();
    var created =
        mvc.perform(
                post("/coupon")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("ABC123"))
            .andReturn();
    var id = json.readTree(created.getResponse().getContentAsString()).get("id").asString();
    mvc.perform(get("/coupon/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    mvc.perform(delete("/coupon/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));
    mvc.perform(get("/coupon/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
    mvc.perform(delete("/coupon/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("COUPON_ALREADY_DELETED"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"expired", "future", "issuer", "audience", "missing-exp"})
  void rejectsInvalidClaims(String variation) throws Exception {
    mvc.perform(
            get("/coupon/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + signed(variation, "coupon:read")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void rejectsTamperedSignatureAndUnsignedToken() throws Exception {
    var parts = signed("valid", "coupon:read").split("\\.");
    parts[2] = (parts[2].charAt(0) == 'A' ? "B" : "A") + parts[2].substring(1);
    mvc.perform(
            get("/coupon/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + String.join(".", parts)))
        .andExpect(status().isUnauthorized());
    String none =
        Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes())
            + "."
            + parts[1]
            + ".";
    mvc.perform(get("/coupon/" + UUID.randomUUID()).header("Authorization", "Bearer " + none))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void distinguishesAuthenticationFromAuthorization() throws Exception {
    var read = signed("valid", "coupon:read");
    mvc.perform(get("/coupon/" + UUID.randomUUID()).header("Authorization", "Bearer " + read))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/coupon")
                .header("Authorization", "Bearer " + read)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"))
        .andExpect(jsonPath("$.correlationId").isString());
  }

  @Test
  void badLoginHasGenericResponseAndNoSession() throws Exception {
    mvc.perform(
            post("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        .andExpect(header().doesNotExist("Set-Cookie"));
    mvc.perform(post("/auth/token").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void healthAndSwaggerWorkWhileH2ConsoleIsAbsent() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
        .andExpect(jsonPath("$.components.schemas.ApiProblem.properties.code.type").value("string"))
        .andExpect(jsonPath("$.paths['/coupon/{id}'].delete.responses['409']").exists())
        .andExpect(
            jsonPath("$.paths['/coupon/{id}'].delete.responses['204'].content").doesNotExist());
    mvc.perform(
            get("/h2-console").header("Authorization", "Bearer " + signed("valid", "coupon:read")))
        .andExpect(status().isNotFound());
  }
}
