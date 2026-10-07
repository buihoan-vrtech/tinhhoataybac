package vn.edu.crs.tinhhoataybac.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.crs.tinhhoataybac.service.*;

@Controller
public class CartController {
  private final CartService cart;
  private final ProductService products;

  public CartController(CartService cart, ProductService products) {
    this.cart = cart;
    this.products = products;
  }

  @GetMapping("/cart")
  public String cart(Model m, HttpSession s) {
    m.addAttribute("cartItems", cart.getCart(s));
    m.addAttribute("cartTotal", cart.getTotal(s));
    m.addAttribute("cartCount", cart.getTotalQuantity(s));
    return "cart";
  }

  @PostMapping("/cart/add")
  public String add(
      @RequestParam Long productId,
      @RequestParam(required = false) Double quantity,
      HttpSession s,
      RedirectAttributes f) {
    try {
      var p = products.getProductById(productId);
      if (p == null) throw new IllegalStateException("Sản phẩm không tồn tại.");
      double q = quantity == null ? p.getMinimumOrderQuantity() : quantity;
      p.validateQuantity(q);
      if (p.getStock() == null || q > p.getStock())
        throw new IllegalStateException("Không đủ tồn kho.");
      double total =
          cart.getCart(s).stream()
                  .filter(i -> i.getProduct().getId().equals(productId))
                  .mapToDouble(i -> i.getQuantity())
                  .sum()
              + q;
      if (total > p.getStock())
        throw new IllegalStateException("Tổng số lượng trong giỏ vượt tồn kho.");
      cart.addToCart(s, p, q);
    } catch (IllegalStateException e) {
      f.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/cart";
  }

  @PostMapping("/cart/update")
  public String update(
      @RequestParam Long productId,
      @RequestParam double quantity,
      HttpSession s,
      RedirectAttributes f) {
    try {
      var p = products.getProductById(productId);
      if (p == null) throw new IllegalStateException("Sản phẩm không tồn tại.");
      if (quantity > 0) {
        p.validateQuantity(quantity);
        if (p.getStock() == null || quantity > p.getStock())
          throw new IllegalStateException("Không đủ tồn kho.");
      }
      cart.updateQuantity(s, productId, quantity);
    } catch (IllegalStateException e) {
      f.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/cart";
  }

  @PostMapping("/cart/remove")
  public String remove(@RequestParam Long productId, HttpSession s) {
    cart.removeFromCart(s, productId);
    return "redirect:/cart";
  }

  @PostMapping("/cart/clear")
  public String clear(HttpSession s) {
    cart.clearCart(s);
    return "redirect:/cart";
  }
}
