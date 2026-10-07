package vn.edu.crs.tinhhoataybac.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import vn.edu.crs.tinhhoataybac.model.Order;
import vn.edu.crs.tinhhoataybac.service.OrderService;
import vn.edu.crs.tinhhoataybac.service.SePayService;

@Controller
public class PaymentController {

    private final vn.edu.crs.tinhhoataybac.service.CommerceService commerce;
    private final vn.edu.crs.tinhhoataybac.service.UserService users;
    private final OrderService orderService;

    private final SePayService sePayService;


    public PaymentController(
            OrderService orderService,
            SePayService sePayService,
            vn.edu.crs.tinhhoataybac.service.CommerceService commerce,
            vn.edu.crs.tinhhoataybac.service.UserService users) {
        this.commerce=commerce;this.users=users;

        this.orderService = orderService;
        this.sePayService = sePayService;
    }


    @GetMapping("/payment/qr/{orderId}")
    public String qrPayment(
            @PathVariable Long orderId,
            Model model, org.springframework.security.core.Authentication authentication) {

        Order order =
                orderService.getOrderById(orderId);


        if (order == null || !commerce.owns(order,users.findByEmail(authentication.getName()))) {
            return "redirect:/";
        }


        if ("CANCELLED".equals(order.getOrderStatus())) return "redirect:/account/orders/"+orderId;
        /*
         * Nếu đơn không dùng QR.
         */
        if (!"QR".equalsIgnoreCase(
                order.getPaymentMethod()
        )) {

            return "redirect:/order-success/"
                    + orderId;
        }


        /*
         * Nếu đã thanh toán rồi.
         */
        if ("PAID".equalsIgnoreCase(
                order.getPaymentStatus()
        )) {

            return "redirect:/order-success/"
                    + orderId;
        }


        model.addAttribute(
                "order",
                order
        );


        model.addAttribute(
                "checkoutUrl",
                sePayService.getCheckoutUrl()
        );


        model.addAttribute(
                "sepayFields",
                sePayService.buildCheckoutFields(order)
        );


        return "sepay-checkout";
    }


    /*
     * SePay redirect về đây sau khi
     * quá trình thanh toán thành công.
     *
     * KHÔNG đánh dấu PAID ở đây.
     *
     * Trạng thái PAID phải lấy từ IPN.
     */
    @GetMapping("/payment/sepay/success/{orderId}")
    public String sePaySuccess(
            @PathVariable Long orderId) {

        return "redirect:/payment/wait/"
                + orderId;
    }


    @GetMapping("/payment/sepay/error/{orderId}")
    public String sePayError(
            @PathVariable Long orderId,
            Model model, org.springframework.security.core.Authentication authentication) {

        model.addAttribute(
                "message",
                "Thanh toán không thành công."
        );

        return "payment-error";
    }


    @GetMapping("/payment/sepay/cancel/{orderId}")
    public String sePayCancel(
            @PathVariable Long orderId,
            Model model, org.springframework.security.core.Authentication authentication) {

        model.addAttribute(
                "message",
                "Bạn đã hủy thanh toán."
        );

        return "payment-error";
    }


    /*
     * Trang đợi IPN xác nhận.
     */
    @GetMapping("/payment/wait/{orderId}")
    public String paymentWait(
            @PathVariable Long orderId,
            Model model, org.springframework.security.core.Authentication authentication) {

        Order order =
                orderService.getOrderById(orderId);


        if (order == null || !commerce.owns(order,users.findByEmail(authentication.getName()))) {
            return "redirect:/";
        }


        if ("PAID".equalsIgnoreCase(
                order.getPaymentStatus()
        )) {

            return "redirect:/order-success/"
                    + orderId;
        }


        model.addAttribute(
                "order",
                order
        );


        return "payment-wait";
    }
}