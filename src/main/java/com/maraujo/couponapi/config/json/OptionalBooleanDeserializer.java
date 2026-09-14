package com.maraujo.couponapi.config.json;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;

/** Missing is false; explicit null and non-boolean values violate the HTTP schema. */
public final class OptionalBooleanDeserializer extends ValueDeserializer<Boolean> {
  @Override
  public Boolean deserialize(JsonParser parser, DeserializationContext context) {
    return switch (parser.currentToken()) {
      case VALUE_TRUE -> true;
      case VALUE_FALSE -> false;
      default -> (Boolean) context.handleUnexpectedToken(Boolean.class, parser);
    };
  }

  @Override
  public Boolean getNullValue(DeserializationContext context) {
    return context.reportInputMismatch(Boolean.class, "Boolean cannot be null.");
  }

  @Override
  public Boolean getAbsentValue(DeserializationContext context) {
    return false;
  }
}
