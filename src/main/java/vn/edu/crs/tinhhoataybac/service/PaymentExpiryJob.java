package vn.edu.crs.tinhhoataybac.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "app.payment-expiry.enabled",
    havingValue = "true",
    matchIfMissing = true)
@Component
public class PaymentExpiryJob {
  private final CommerceService commerce;

  public PaymentExpiryJob(CommerceService commerce) {
    this.commerce = commerce;
  }

  @Scheduled(fixedDelay = 30000)
  public void expire() {
    commerce.expirePendingPayments();
  }
}
