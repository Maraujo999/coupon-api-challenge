package com.maraujo.couponapi.config;

import org.springframework.boot.jackson.autoconfigure.*;
import org.springframework.context.annotation.*;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.databind.*;

@Configuration(proxyBeanMethods = false)
public class JsonConfiguration {
  @Bean
  JsonMapperBuilderCustomizer jsonMapperCustomizer() {
    return builder ->
        builder
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  @Bean
  JsonFactoryBuilderCustomizer jsonFactoryCustomizer() {
    return builder ->
        builder.streamReadConstraints(
            StreamReadConstraints.builder()
                .maxNumberLength(1000)
                .maxNestingDepth(20)
                .maxStringLength(65536)
                .build());
  }
}
