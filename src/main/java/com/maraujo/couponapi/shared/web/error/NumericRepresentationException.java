package com.maraujo.couponapi.shared.web.error;

public final class NumericRepresentationException extends RuntimeException {
  public NumericRepresentationException() {
    super(
        "O número excede a representação suportada: até 1000 dígitos significativos e expoente ajustado entre -1000 e 1000.");
  }
}
