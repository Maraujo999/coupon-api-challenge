package com.maraujo.couponapi.coupon.adapter.in.web.dto;

import com.maraujo.couponapi.config.json.OptionalBooleanDeserializer;
import com.maraujo.couponapi.shared.web.error.NumericRepresentationException;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import tools.jackson.databind.annotation.JsonDeserialize;

public record CreateCouponRequest(
    @NotNull String code,
    @NotNull String description,
    @NotNull BigDecimal discountValue,
    @NotNull OffsetDateTime expirationDate,
    @JsonDeserialize(using = OptionalBooleanDeserializer.class) Boolean published) {
  public CreateCouponRequest {
    published = published == null ? false : published;
  }

  public void validateRepresentation() {
    long adjustedExponent = (long) discountValue.precision() - discountValue.scale() - 1;
    if (discountValue.precision() > 1000 || Math.abs(adjustedExponent) > 1000) {
      throw new NumericRepresentationException();
    }
  }
}
