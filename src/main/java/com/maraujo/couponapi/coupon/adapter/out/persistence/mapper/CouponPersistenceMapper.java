package com.maraujo.couponapi.coupon.adapter.out.persistence.mapper;

import com.maraujo.couponapi.config.mapper.MapperConfiguration;
import com.maraujo.couponapi.coupon.adapter.out.persistence.entity.CouponJpaEntity;
import com.maraujo.couponapi.coupon.domain.model.*;
import org.mapstruct.*;

@Mapper(config = MapperConfiguration.class)
public interface CouponPersistenceMapper {
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "code", source = "code.value")
  @Mapping(target = "discountValue", source = "discountValue.value")
  CouponJpaEntity toEntity(CouponSnapshot snapshot);

  @Mapping(target = "code", expression = "java(new CouponCode(entity.getCode()))")
  @Mapping(
      target = "discountValue",
      expression = "java(new DiscountValue(entity.getDiscountValue()))")
  CouponSnapshot toSnapshot(CouponJpaEntity entity);
}
