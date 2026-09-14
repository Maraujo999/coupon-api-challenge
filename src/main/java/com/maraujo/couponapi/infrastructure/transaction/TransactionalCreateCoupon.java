package com.maraujo.couponapi.infrastructure.transaction;

import com.maraujo.couponapi.coupon.application.command.CreateCouponCommand;
import com.maraujo.couponapi.coupon.application.port.in.CreateCouponUseCase;
import com.maraujo.couponapi.coupon.domain.model.Coupon;
import org.slf4j.*;
import org.springframework.transaction.support.TransactionTemplate;

public final class TransactionalCreateCoupon implements CreateCouponUseCase {
  private static final Logger LOG = LoggerFactory.getLogger(TransactionalCreateCoupon.class);
  private final CreateCouponUseCase delegate;
  private final TransactionTemplate transactions;

  public TransactionalCreateCoupon(CreateCouponUseCase delegate, TransactionTemplate transactions) {
    this.delegate = delegate;
    this.transactions = transactions;
  }

  @Override
  public Coupon execute(CreateCouponCommand command) {
    var coupon = transactions.execute(status -> delegate.execute(command));
    LOG.info("coupon_created couponId={}", coupon.snapshot().id());
    return coupon;
  }
}
