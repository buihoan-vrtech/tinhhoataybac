package vn.edu.crs.tinhhoataybac.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.crs.tinhhoataybac.model.Order;
import vn.edu.crs.tinhhoataybac.service.CartService;
import vn.edu.crs.tinhhoataybac.service.OrderService;

@Controller
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;

    public CheckoutController(CartService cartService,
                              OrderService orderService) {

        this.cartService = cartService;
        this.orderService = orderService;
    }

    @GetMapping("/checkout")
    public String checkout(Model model,
                           HttpSession session) {

        if (cartService.getCart(session).isEmpty()) {

            return "redirect:/cart";
        }

        model.addAttribute(
                "cartItems",
                cartService.getCart(session)
        );

        model.addAttribute(
                "cartTotal",
                cartService.getTotal(session)
        );

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
            HttpSession session,
            Model model) {

        try {

            if (customerName == null
                    || customerName.trim().isEmpty()) {

                throw new IllegalStateException(
                        "Vui lòng nhập họ tên."
                );
            }

            if (phone == null
                    || phone.trim().isEmpty()) {

                throw new IllegalStateException(
                        "Vui lòng nhập số điện thoại."
                );
            }

            if (address == null
                    || address.trim().isEmpty()) {

                throw new IllegalStateException(
                        "Vui lòng nhập địa chỉ nhận hàng."
                );
            }

            if (!paymentMethod.equals("COD")
                    && !paymentMethod.equals("QR")
                    && !paymentMethod.equals("WALLET")) {

                throw new IllegalStateException(
                        "Phương thức thanh toán không hợp lệ."
                );
            }

            if (paymentMethod.equals("WALLET")) {

                model.addAttribute(
                        "error",
                        "Thanh toán bằng Ví sẽ được kích hoạt "
                                + "sau khi hoàn thiện tài khoản đăng nhập."
                );

                model.addAttribute(
                        "cartItems",
                        cartService.getCart(session)
                );

                model.addAttribute(
                        "cartTotal",
                        cartService.getTotal(session)
                );

                return "checkout";
            }

            Order order =
                    orderService.createOrder(
                            customerName.trim(),
                            phone.trim(),
                            email,
                            address.trim(),
                            note,
                            paymentMethod,
                            cartService.getCart(session)
                    );

            cartService.clearCart(session);

            if ("QR".equals(paymentMethod)) {

                return "redirect:/payment/qr/"
                        + order.getId();
            }

            return "redirect:/order-success/"
                    + order.getId();

        } catch (Exception e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            model.addAttribute(
                    "cartItems",
                    cartService.getCart(session)
            );

            model.addAttribute(
                    "cartTotal",
                    cartService.getTotal(session)
            );

            return "checkout";
        }
    }

    @GetMapping("/order-success/{id}")
    public String orderSuccess(
            @PathVariable Long id,
            Model model) {

        Order order =
                orderService.getOrderById(id);

        if (order == null) {

            return "redirect:/";
        }

        model.addAttribute(
                "order",
                order
        );

        return "order-success";
    }
}