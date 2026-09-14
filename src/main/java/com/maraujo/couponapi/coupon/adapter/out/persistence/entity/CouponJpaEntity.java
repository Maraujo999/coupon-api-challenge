package com.maraujo.couponapi.coupon.adapter.out.persistence.entity;

import com.maraujo.couponapi.coupon.domain.model.CouponStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coupon")
public class CouponJpaEntity {
  @Id private UUID id;

  @Column(nullable = false, length = 6)
  private String code;

  @Lob
  @Column(nullable = false)
  private String description;

  @Column(name = "discount_value", nullable = false, columnDefinition = "DECFLOAT(1000)")
  private BigDecimal discountValue;

  @Column(
      name = "expiration_date",
      nullable = false,
      columnDefinition = "TIMESTAMP(9) WITH TIME ZONE")
  private Instant expirationDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 8)
  private CouponStatus status;

  @Column(nullable = false)
  private boolean published;

  @Column(nullable = false)
  private boolean redeemed;

  @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP(9) WITH TIME ZONE")
  private Instant createdAt;

  @Column(name = "deleted_at", columnDefinition = "TIMESTAMP(9) WITH TIME ZONE")
  private Instant deletedAt;

  @Version
  @Column(nullable = false)
  private Long version;

  protected CouponJpaEntity() {}

  public CouponJpaEntity(
      UUID id,
      String code,
      String description,
      BigDecimal discountValue,
      Instant expirationDate,
      CouponStatus status,
      boolean published,
      boolean redeemed,
      Instant createdAt,
      Instant deletedAt,
      Long version) {
    this.id = id;
    this.code = code;
    this.description = description;
    this.discountValue = discountValue;
    this.expirationDate = expirationDate;
    this.status = status;
    this.published = published;
    this.redeemed = redeemed;
    this.createdAt = createdAt;
    this.deletedAt = deletedAt;
    this.version = version;
  }

  public UUID getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getDiscountValue() {
    return discountValue;
  }

  public Instant getExpirationDate() {
    return expirationDate;
  }

  public CouponStatus getStatus() {
    return status;
  }

  public boolean isPublished() {
    return published;
  }

  public boolean isRedeemed() {
    return redeemed;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public Long getVersion() {
    return version;
  }
}
