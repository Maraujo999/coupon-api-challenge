package com.maraujo.couponapi.coupon.adapter.in.web.mapper;

import com.maraujo.couponapi.config.mapper.MapperConfiguration;
import com.maraujo.couponapi.coupon.adapter.in.web.dto.*;
import com.maraujo.couponapi.coupon.application.command.CreateCouponCommand;
import com.maraujo.couponapi.coupon.domain.model.CouponSnapshot;
import java.time.*;
import org.mapstruct.*;

@Mapper(config = MapperConfiguration.class)
public interface CouponWebMapper {
  CreateCouponCommand toCommand(CreateCouponRequest request);

  @Mapping(target = "code", source = "code.value")
  @Mapping(target = "discountValue", source = "discountValue.value")
  CouponResponse toResponse(CouponSnapshot snapshot);

  default Instant toInstant(OffsetDateTime value) {
    return value == null ? null : value.toInstant();
  }
}
