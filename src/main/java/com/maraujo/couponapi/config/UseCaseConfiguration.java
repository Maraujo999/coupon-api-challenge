package com.maraujo.couponapi.config;

import com.maraujo.couponapi.coupon.application.port.in.*;
import com.maraujo.couponapi.coupon.application.port.out.CouponRepository;
import com.maraujo.couponapi.coupon.application.service.*;
import com.maraujo.couponapi.infrastructure.transaction.*;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
public class UseCaseConfiguration {
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  CreateCouponUseCase createCoupon(
      CouponRepository repository, Clock clock, PlatformTransactionManager manager) {
    return new TransactionalCreateCoupon(
        new CreateCouponService(repository, clock, UUID::randomUUID),
        new TransactionTemplate(manager));
  }

  @Bean
  GetCouponUseCase getCoupon(CouponRepository repository) {
    return new GetCouponService(repository);
  }

  @Bean
  DeleteCouponUseCase deleteCoupon(
      CouponRepository repository, Clock clock, PlatformTransactionManager manager) {
    return new TransactionalDeleteCoupon(
        new DeleteCouponService(repository, clock), new TransactionTemplate(manager));
  }
}
