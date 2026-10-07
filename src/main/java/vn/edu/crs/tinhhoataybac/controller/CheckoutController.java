package vn.edu.crs.tinhhoataybac.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.crs.tinhhoataybac.model.*;
import vn.edu.crs.tinhhoataybac.repository.*;
import vn.edu.crs.tinhhoataybac.service.*;

@Controller
public class CheckoutController {
  private final CartService cart;
  private final CommerceService commerce;
  private final UserService users;
  private final WalletService wallets;
  private final OrderRepository orders;
  private final UserAddressRepository addresses;

  public CheckoutController(
      CartService cart,
      CommerceService commerce,
      UserService users,
      WalletService wallets,
      OrderRepository orders,
      UserAddressRepository addresses) {
    this.cart = cart;
    this.commerce = commerce;
    this.users = users;
    this.wallets = wallets;
    this.orders = orders;
    this.addresses = addresses;
  }

  private void fill(Model m, HttpSession s, User u) {
    m.addAttribute("cartItems", cart.getCart(s));
    m.addAttribute("cartTotal", cart.getTotal(s));
    m.addAttribute("user", u);
    m.addAttribute("walletBalance", wallets.getBalance(u));
    m.addAttribute("addresses", addresses.findByUserIdOrderByDefaultAddressDescIdDesc(u.getId()));
  }

  @GetMapping("/checkout")
  public String checkout(Model m, HttpSession s, Authentication a) {
    if (cart.getCart(s).isEmpty()) return "redirect:/cart";
    fill(m, s, users.findByEmail(a.getName()));
    return "checkout";
  }

  @PostMapping("/checkout/place-order")
  public String placeOrder(
      @RequestParam String customerName,
      @RequestParam String phone,
      @RequestParam(required = false) String email,
      @RequestParam String address,
      @RequestParam(required = false) String note,
      @RequestParam String paymentMethod,
      @RequestParam(defaultValue = "standard") String shippingMethod,
      @RequestParam(required = false) String voucherCode,
      HttpSession s,
      Model m,
      Authentication a) {
    User u = users.findByEmail(a.getName());
    try {
      Order o =
          commerce.checkout(
              u,
              customerName,
              phone,
              address,
              note,
              paymentMethod,
              cart.getCart(s),
              shippingMethod,
              voucherCode);
      cart.clearCart(s);
      return "redirect:"
          + ("QR".equals(paymentMethod) ? "/payment/qr/" : "/order-success/")
          + o.getId();
    } catch (IllegalStateException | IllegalArgumentException e) {
      fill(m, s, u);
      m.addAttribute("error", e.getMessage());
      return "checkout";
    }
  }

  @GetMapping("/order-success/{id}")
  public String success(@PathVariable Long id, Authentication a, Model m) {
    Order o = orders.findById(id).orElse(null);
    if (!commerce.owns(o, users.findByEmail(a.getName()))) return "redirect:/account/orders";
    m.addAttribute("order", o);
    return "order-success";
  }

  @PostMapping("/checkout/quote")
  @ResponseBody
  public org.springframework.http.ResponseEntity<?> quote(
      @RequestParam(defaultValue = "standard") String shippingMethod,
      @RequestParam(required = false) String voucherCode,
      HttpSession session) {
    try {
      return org.springframework.http.ResponseEntity.ok(
          commerce.quote(cart.getCart(session), shippingMethod, voucherCode));
    } catch (IllegalStateException e) {
      return org.springframework.http.ResponseEntity.badRequest()
          .body(java.util.Map.of("error", e.getMessage()));
    }
  }
}
