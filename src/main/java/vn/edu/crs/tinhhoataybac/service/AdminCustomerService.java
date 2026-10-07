package vn.edu.crs.tinhhoataybac.service;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.crs.tinhhoataybac.model.*;
import vn.edu.crs.tinhhoataybac.repository.*;

@Service
public class AdminCustomerService {
  private final UserRepository users;
  private final WalletService wallets;
  private final WalletTransactionRepository transactions;
  private final WalletTopUpRepository topUps;
  private final WalletTopUpService topUpService;

  public AdminCustomerService(
      UserRepository users,
      WalletService wallets,
      WalletTransactionRepository transactions,
      WalletTopUpRepository topUps,
      WalletTopUpService topUpService) {
    this.users = users;
    this.wallets = wallets;
    this.transactions = transactions;
    this.topUps = topUps;
    this.topUpService = topUpService;
  }

  @Transactional
  public void adjust(Long id, String direction, BigDecimal amount, String note, String actor) {
    if (!java.util.Set.of("credit", "debit").contains(direction)
        || amount == null
        || amount.signum() <= 0
        || amount.compareTo(new BigDecimal("100000000")) > 0
        || amount.stripTrailingZeros().scale() > 2
        || note == null
        || note.isBlank()
        || note.length() > 500)
      throw new IllegalStateException("Nhập số tiền hợp lệ và lý do điều chỉnh.");
    User u = users.lockById(id).orElseThrow();
    if (!"USER".equals(u.getRole()))
      throw new IllegalStateException("Chỉ điều chỉnh ví khách hàng.");
    Wallet w = wallets.getOrCreateWallet(u);
    BigDecimal signed = "credit".equals(direction) ? amount : amount.negate();
    if (w.getBalance().add(signed).signum() < 0)
      throw new IllegalStateException("Số dư không đủ để trừ.");
    if (signed.signum() > 0)
      wallets.deposit(u, amount, note + " · Admin: " + actor, "ADM-" + UUID.randomUUID());
    else wallets.pay(u, amount, note + " · Admin: " + actor, "ADM-" + UUID.randomUUID());
  }

  @Transactional
  public void reviewTopUp(
      Long customerId, Long topUpId, String decision, String note, boolean received, String actor) {
    WalletTopUp t = topUps.lockById(topUpId).orElseThrow();
    if (!t.getUser().getId().equals(customerId))
      throw new IllegalStateException("Yêu cầu nạp không thuộc khách hàng.");
    if (!"PENDING".equals(t.getStatus())) throw new IllegalStateException("Yêu cầu đã được xử lý.");
    if (note == null || note.isBlank() || note.length() > 500)
      throw new IllegalStateException("Nhập lý do xử lý.");
    if ("approve".equals(decision)) {
      if (!received)
        throw new IllegalStateException("Cần xác nhận đã nhận đủ tiền trước khi duyệt.");
      topUpService.confirmTopUp(t.getPaymentCode(), t.getAmount());
    } else if ("reject".equals(decision)) {
      t.setStatus("REJECTED");
      topUps.save(t);
    } else throw new IllegalStateException("Quyết định không hợp lệ.");
    WalletTransaction audit = new WalletTransaction();
    audit.setWallet(wallets.getOrCreateWallet(t.getUser()));
    audit.setType("ADMIN_REVIEW");
    audit.setAmount(BigDecimal.ZERO);
    audit.setBalanceAfter(audit.getWallet().getBalance());
    audit.setStatus("SUCCESS");
    audit.setReferenceCode("REVIEW-" + t.getId());
    audit.setDescription(decision + ": " + note + " · Admin: " + actor);
    transactions.save(audit);
  }
}
