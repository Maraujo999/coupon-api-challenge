package com.maraujo.couponapi.config.mapper;

import org.mapstruct.*;

@MapperConfig(
    componentModel = MappingConstants.ComponentModel.SPRING,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MapperConfiguration {}
