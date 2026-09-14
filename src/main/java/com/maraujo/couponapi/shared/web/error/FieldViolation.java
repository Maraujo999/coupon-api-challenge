package com.maraujo.couponapi.shared.web.error;

public record FieldViolation(String field, String code, String message) {}
