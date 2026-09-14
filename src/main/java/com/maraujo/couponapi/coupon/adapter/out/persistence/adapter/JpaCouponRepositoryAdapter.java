package com.maraujo.couponapi.coupon.adapter.out.persistence.adapter;

import com.maraujo.couponapi.coupon.adapter.out.persistence.mapper.CouponPersistenceMapper;
import com.maraujo.couponapi.coupon.adapter.out.persistence.repository.SpringDataCouponRepository;
import com.maraujo.couponapi.coupon.application.exception.ConcurrentCouponModificationException;
import com.maraujo.couponapi.coupon.application.port.out.*;
import com.maraujo.couponapi.coupon.domain.model.Coupon;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCouponRepositoryAdapter implements CouponRepository {
  private final SpringDataCouponRepository repository;
  private final CouponPersistenceMapper mapper;

  public JpaCouponRepositoryAdapter(
      SpringDataCouponRepository repository, CouponPersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public void insert(Coupon coupon) {
    repository.saveAndFlush(mapper.toEntity(coupon.snapshot()));
  }

  @Override
  public Optional<StoredCoupon> findById(UUID id) {
    return repository
        .findById(id)
        .map(
            entity ->
                new StoredCoupon(Coupon.restore(mapper.toSnapshot(entity)), entity.getVersion()));
  }

  @Override
  public void update(Coupon coupon, long expectedVersion) {
    var snapshot = coupon.snapshot();
    if (repository.markDeleted(snapshot.id(), snapshot.deletedAt(), expectedVersion) != 1) {
      throw new ConcurrentCouponModificationException();
    }
  }
}
