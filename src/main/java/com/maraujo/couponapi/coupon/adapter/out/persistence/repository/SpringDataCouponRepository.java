package com.maraujo.couponapi.coupon.adapter.out.persistence.repository;

import com.maraujo.couponapi.coupon.adapter.out.persistence.entity.CouponJpaEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SpringDataCouponRepository extends JpaRepository<CouponJpaEntity, UUID> {
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
        update CouponJpaEntity c set c.status = com.maraujo.couponapi.coupon.domain.model.CouponStatus.DELETED,
            c.deletedAt = :deletedAt, c.version = c.version + 1
        where c.id = :id and c.version = :version
            and c.status <> com.maraujo.couponapi.coupon.domain.model.CouponStatus.DELETED
        """)
  int markDeleted(
      @Param("id") UUID id, @Param("deletedAt") Instant deletedAt, @Param("version") long version);
}
