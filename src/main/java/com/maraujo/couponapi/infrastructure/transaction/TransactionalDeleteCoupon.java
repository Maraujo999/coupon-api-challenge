package com.maraujo.couponapi.infrastructure.transaction;

import com.maraujo.couponapi.coupon.application.port.in.DeleteCouponUseCase;
import java.util.UUID;
import org.slf4j.*;
import org.springframework.transaction.support.TransactionTemplate;

public final class TransactionalDeleteCoupon implements DeleteCouponUseCase {
  private static final Logger LOG = LoggerFactory.getLogger(TransactionalDeleteCoupon.class);
  private final DeleteCouponUseCase delegate;
  private final TransactionTemplate transactions;

  public TransactionalDeleteCoupon(DeleteCouponUseCase delegate, TransactionTemplate transactions) {
    this.delegate = delegate;
    this.transactions = transactions;
  }

  @Override
  public void execute(UUID id) {
    transactions.executeWithoutResult(status -> delegate.execute(id));
    LOG.info("coupon_deleted couponId={}", id);
  }
}
