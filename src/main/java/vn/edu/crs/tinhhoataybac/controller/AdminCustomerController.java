package vn.edu.crs.tinhhoataybac.controller;

import java.math.BigDecimal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.crs.tinhhoataybac.repository.*;
import vn.edu.crs.tinhhoataybac.service.*;

@Controller
@RequestMapping("/admin")
public class AdminCustomerController {
  private final UserRepository users;
  private final OrderRepository orders;
  private final WalletService wallets;
  private final WalletTopUpRepository topUps;
  private final AdminCustomerService admin;
  private final ReviewRepository reviews;

  public AdminCustomerController(
      UserRepository users,
      OrderRepository orders,
      WalletService wallets,
      WalletTopUpRepository topUps,
      AdminCustomerService admin,
      ReviewRepository reviews) {
    this.users = users;
    this.orders = orders;
    this.wallets = wallets;
    this.topUps = topUps;
    this.admin = admin;
    this.reviews = reviews;
  }

  @GetMapping("/customers")
  public String list(@RequestParam(defaultValue = "") String keyword, Model m) {
    String q = keyword.toLowerCase(java.util.Locale.ROOT).trim();
    m.addAttribute(
        "customers",
        users.findAll().stream()
            .filter(u -> "USER".equals(u.getRole()))
            .filter(
                u ->
                    q.isEmpty()
                        || u.getEmail().toLowerCase(java.util.Locale.ROOT).contains(q)
                        || u.getFullName().toLowerCase(java.util.Locale.ROOT).contains(q))
            .toList());
    m.addAttribute("keyword", keyword);
    return "admin/customers";
  }

  @GetMapping("/customers/{id}")
  public String detail(@PathVariable Long id, Model m) {
    var u = users.findById(id).orElseThrow();
    var w = wallets.getOrCreateWallet(u);
    m.addAttribute("customer", u);
    m.addAttribute("wallet", w);
    m.addAttribute("transactions", wallets.getTransactions(w));
    m.addAttribute("topUps", topUps.findByUserIdOrderByCreatedAtDesc(id));
    m.addAttribute("orders", orders.findByEmailIgnoreCaseOrderByCreatedAtDesc(u.getEmail()));
    return "admin/customer-detail";
  }

  @PostMapping("/customers/{id}/wallet/adjustment")
  public String adjust(
      @PathVariable Long id,
      @RequestParam String direction,
      @RequestParam BigDecimal amount,
      @RequestParam String note,
      Authentication a,
      RedirectAttributes f) {
    try {
      admin.adjust(id, direction, amount, note, a.getName());
      f.addFlashAttribute("success", "Đã điều chỉnh ví và lưu lịch sử.");
    } catch (RuntimeException e) {
      f.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/admin/customers/" + id;
  }

  @PostMapping("/customers/{id}/top-ups/{topUpId}/review")
  public String review(
      @PathVariable Long id,
      @PathVariable Long topUpId,
      @RequestParam String decision,
      @RequestParam String note,
      @RequestParam(defaultValue = "false") boolean received,
      Authentication a,
      RedirectAttributes f) {
    try {
      admin.reviewTopUp(id, topUpId, decision, note, received, a.getName());
      f.addFlashAttribute("success", "Đã xử lý yêu cầu nạp.");
    } catch (RuntimeException e) {
      f.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/admin/customers/" + id;
  }

  @GetMapping("/reviews")
  public String reviews(Model m) {
    m.addAttribute("reviews", reviews.findAll());
    return "admin/reviews";
  }

  @PostMapping("/reviews/{id}/delete")
  public String deleteReview(@PathVariable Long id) {
    reviews.deleteById(id);
    return "redirect:/admin/reviews";
  }
}
