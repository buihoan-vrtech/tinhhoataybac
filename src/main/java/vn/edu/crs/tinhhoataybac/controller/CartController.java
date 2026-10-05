package vn.edu.crs.tinhhoataybac.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.crs.tinhhoataybac.model.Product;
import vn.edu.crs.tinhhoataybac.service.CartService;
import vn.edu.crs.tinhhoataybac.service.ProductService;

@Controller
public class CartController {

    private final CartService cartService;
    private final ProductService productService;

    public CartController(CartService cartService,
                          ProductService productService) {
        this.cartService = cartService;
        this.productService = productService;
    }


    // =========================
    // HIỂN THỊ GIỎ HÀNG
    // =========================
    @GetMapping("/cart")
    public String cart(Model model,
                       HttpSession session) {

        model.addAttribute(
                "cartItems",
                cartService.getCart(session)
        );

        model.addAttribute(
                "cartTotal",
                cartService.getTotal(session)
        );

        model.addAttribute(
                "cartCount",
                cartService.getTotalQuantity(session)
        );

        return "cart";
    }


    // =========================
    // THÊM VÀO GIỎ
    // =========================
    @PostMapping("/cart/add")
    public String addToCart(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int quantity,
            HttpSession session) {

        Product product =
                productService.getProductById(productId);

        if (product == null) {
            return "redirect:/products";
        }

        if (quantity < 1) {
            quantity = 1;
        }

        if (product.getStock() != null
                && quantity > product.getStock()) {

            quantity = product.getStock();
        }

        cartService.addToCart(
                session,
                product,
                quantity
        );

        return "redirect:/cart";
    }


    // =========================
    // CẬP NHẬT SỐ LƯỢNG
    // =========================
    @PostMapping("/cart/update")
    public String updateCart(
            @RequestParam Long productId,
            @RequestParam int quantity,
            HttpSession session) {

        Product product =
                productService.getProductById(productId);

        if (product == null) {
            return "redirect:/cart";
        }

        if (product.getStock() != null
                && quantity > product.getStock()) {

            quantity = product.getStock();
        }

        cartService.updateQuantity(
                session,
                productId,
                quantity
        );

        return "redirect:/cart";
    }


    // =========================
    // XÓA 1 SẢN PHẨM
    // =========================
    @PostMapping("/cart/remove")
    public String removeFromCart(
            @RequestParam Long productId,
            HttpSession session) {

        cartService.removeFromCart(
                session,
                productId
        );

        return "redirect:/cart";
    }


    // =========================
    // XÓA TOÀN BỘ GIỎ
    // =========================
    @PostMapping("/cart/clear")
    public String clearCart(
            HttpSession session) {

        cartService.clearCart(session);

        return "redirect:/cart";
    }
}