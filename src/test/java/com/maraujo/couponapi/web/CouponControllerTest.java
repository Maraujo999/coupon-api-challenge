package com.maraujo.couponapi.web;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.maraujo.couponapi.config.JsonConfiguration;
import com.maraujo.couponapi.coupon.adapter.in.web.CouponController;
import com.maraujo.couponapi.coupon.adapter.in.web.mapper.CouponWebMapperImpl;
import com.maraujo.couponapi.coupon.application.command.CreateCouponCommand;
import com.maraujo.couponapi.coupon.application.exception.*;
import com.maraujo.couponapi.coupon.application.port.in.*;
import com.maraujo.couponapi.coupon.domain.model.Coupon;
import com.maraujo.couponapi.shared.web.error.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(CouponController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
  CouponWebMapperImpl.class,
  ProblemDetailFactory.class,
  ProblemResponseWriter.class,
  GlobalExceptionHandler.class,
  JsonConfiguration.class
})
class CouponControllerTest {
  static final UUID ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
  static final Instant NOW = Instant.parse("2026-09-11T20:00:00Z");
  @Autowired MockMvc mvc;
  @Autowired JsonMapper json;
  @MockitoBean CreateCouponUseCase create;
  @MockitoBean GetCouponUseCase get;
  @MockitoBean DeleteCouponUseCase delete;

  String body() {
    return """
            {"code":"ABC-123","description":"Original","discountValue":0.5001,
             "expirationDate":"2030-01-01T09:00:00.123456789-03:00"}
            """;
  }

  Coupon coupon() {
    return Coupon.create(
        ID,
        "ABC-123",
        "Original",
        new BigDecimal("0.5001"),
        Instant.parse("2030-01-01T12:00:00.123456789Z"),
        false,
        NOW);
  }

  @BeforeEach
  void useRealDomainRulesForCreation() {
    when(create.execute(any()))
        .thenAnswer(
            inv -> {
              CreateCouponCommand c = inv.getArgument(0);
              return Coupon.create(
                  ID,
                  c.code(),
                  c.description(),
                  c.discountValue(),
                  c.expirationDate(),
                  c.published(),
                  NOW);
            });
  }

  @Test
  void createsExactContractWithLocationAndDefaults() throws Exception {
    var response =
        mvc.perform(post("/coupon").contentType(MediaType.APPLICATION_JSON).content(body()))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/coupon/" + ID))
            .andExpect(jsonPath("$.code").value("ABC123"))
            .andExpect(jsonPath("$.discountValue").value(0.5001))
            .andExpect(jsonPath("$.expirationDate").value("2030-01-01T12:00:00.123456789Z"))
            .andExpect(jsonPath("$.published").value(false))
            .andExpect(jsonPath("$.redeemed").value(false))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andReturn();
    assertThat(json.readTree(response.getResponse().getContentAsString()).size()).isEqualTo(8);
  }

  @Test
  void allowsPublishedTrueAndIgnoresOutputOnlyFields() throws Exception {
    var body = json.readTree(body()).deepCopy();
    ((tools.jackson.databind.node.ObjectNode) body)
        .put("published", true)
        .put("redeemed", true)
        .put("status", "DELETED")
        .put("id", UUID.randomUUID().toString());
    mvc.perform(post("/coupon").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.published").value(true))
        .andExpect(jsonPath("$.redeemed").value(false))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.id").value(ID.toString()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"code", "description", "discountValue", "expirationDate"})
  void rejectsMissingAndNullRequiredFields(String field) throws Exception {
    var node = (tools.jackson.databind.node.ObjectNode) json.readTree(body());
    node.remove(field);
    mvc.perform(post("/coupon").contentType(MediaType.APPLICATION_JSON).content(node.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    node.putNull(field);
    mvc.perform(post("/coupon").contentType(MediaType.APPLICATION_JSON).content(node.toString()))
        .andExpect(status().isBadRequest());
    verify(create, never()).execute(any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"{", "[]", "{\"published\":null}", "{\"discountValue\":\"oops\"}"})
  void rejectsMalformedStructure(String body) throws Exception {
    mvc.perform(post("/coupon").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void rejectsExplicitNullPublishedAndWrongTypes() throws Exception {
    var node = (tools.jackson.databind.node.ObjectNode) json.readTree(body());
    node.putNull("published");
    mvc.perform(post("/coupon").contentType(MediaType.APPLICATION_JSON).content(node.toString()))
        .andExpect(status().isBadRequest());
    node.put("published", "true");
    mvc.perform(post("/coupon").contentType(MediaType.APPLICATION_JSON).content(node.toString()))
        .andExpect(status().isBadRequest());
    node.put("published", false).put("discountValue", "1.5");
    mvc.perform(post("/coupon").contentType(MediaType.APPLICATION_JSON).content(node.toString()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsMissingTimezone() throws Exception {
    mvc.perform(
            post("/coupon")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body().replace("09:00:00.123456789-03:00", "09:00:00")))
        .andExpect(status().isBadRequest());
  }

  @ParameterizedTest
  @CsvSource({"ABC-123,0.49,INVALID_DISCOUNT_VALUE", "ABC12,1,INVALID_COUPON_CODE"})
  void mapsBusinessViolationsTo422(String code, String discount, String error) throws Exception {
    mvc.perform(
            post("/coupon")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body().replace("ABC-123", code).replace("0.5001", discount)))
        .andExpect(status().is(422))
        .andExpect(jsonPath("$.code").value(error))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.correlationId").isString());
  }

  @Test
  void rejectsBlankDescriptionAndPastDate() throws Exception {
    mvc.perform(
            post("/coupon")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body().replace("Original", "  ")))
        .andExpect(status().is(422))
        .andExpect(jsonPath("$.code").value("INVALID_COUPON_DESCRIPTION"));
    mvc.perform(
            post("/coupon")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body().replace("2030", "2020")))
        .andExpect(status().is(422))
        .andExpect(jsonPath("$.code").value("EXPIRATION_IN_PAST"));
  }

  @Test
  void rejectsUnrepresentableNumericInputInsteadOfRounding() throws Exception {
    mvc.perform(
            post("/coupon")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body().replace("0.5001", "1e1001")))
        .andExpect(status().is(422))
        .andExpect(jsonPath("$.code").value("NUMBER_OUT_OF_RANGE"));
  }

  @Test
  void readsSameIdentifierAndReturns404ForMissing() throws Exception {
    when(get.execute(ID)).thenReturn(coupon());
    mvc.perform(get("/coupon/{id}", ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ID.toString()));
    when(get.execute(ID)).thenThrow(new CouponNotFoundException());
    mvc.perform(get("/coupon/{id}", ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COUPON_NOT_FOUND"));
    mvc.perform(get("/coupon/not-a-uuid")).andExpect(status().isBadRequest());
  }

  @Test
  void deletesWithoutBodyAndMapsConflicts() throws Exception {
    mvc.perform(delete("/coupon/{id}", ID))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));
    doThrow(new ConcurrentCouponModificationException()).when(delete).execute(ID);
    mvc.perform(delete("/coupon/{id}", ID))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("COUPON_CONCURRENT_MODIFICATION"));
  }

  @Test
  void unexpectedErrorDoesNotLeakInternalDetails() throws Exception {
    when(get.execute(ID)).thenThrow(new IllegalStateException("internal SQL detail"));
    mvc.perform(get("/coupon/{id}", ID))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("internal SQL detail"))));
  }

  @Test
  void preservesMethodAndMediaStatusCodes() throws Exception {
    mvc.perform(put("/coupon"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().exists("Allow"));
    mvc.perform(post("/coupon").contentType(MediaType.TEXT_PLAIN).content(body()))
        .andExpect(status().isUnsupportedMediaType());
    when(get.execute(ID)).thenReturn(coupon());
    mvc.perform(get("/coupon/{id}", ID).accept(MediaType.IMAGE_PNG))
        .andExpect(status().isNotAcceptable());
  }
}
