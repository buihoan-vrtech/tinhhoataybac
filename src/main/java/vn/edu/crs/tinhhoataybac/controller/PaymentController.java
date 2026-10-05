package vn.edu.crs.tinhhoataybac.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.crs.tinhhoataybac.model.Order;
import vn.edu.crs.tinhhoataybac.service.OrderService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class PaymentController {

    private final OrderService orderService;

    @Value("${payment.qr.bank-id}")
    private String bankId;

    @Value("${payment.qr.account-no}")
    private String accountNo;

    @Value("${payment.qr.account-name}")
    private String accountName;

    public PaymentController(
            OrderService orderService) {

        this.orderService = orderService;
    }

    @GetMapping("/payment/qr/{orderId}")
    public String qrPayment(
            @PathVariable Long orderId,
            Model model) {

        Order order =
                orderService.getOrderById(orderId);

        if (order == null) {
            return "redirect:/";
        }

        if (!"QR".equals(order.getPaymentMethod())) {
            return "redirect:/order-success/"
                    + orderId;
        }

        String encodedInfo =
                URLEncoder.encode(
                        order.getPaymentCode(),
                        StandardCharsets.UTF_8
                );

        String encodedName =
                URLEncoder.encode(
                        accountName,
                        StandardCharsets.UTF_8
                );

        String qrUrl =
                "https://img.vietqr.io/image/"
                        + bankId
                        + "-"
                        + accountNo
                        + "-compact2.png"
                        + "?amount="
                        + order.getTotalAmount()
                        .toBigInteger()
                        + "&addInfo="
                        + encodedInfo
                        + "&accountName="
                        + encodedName;

        model.addAttribute(
                "order",
                order
        );

        model.addAttribute(
                "qrUrl",
                qrUrl
        );

        model.addAttribute(
                "bankId",
                bankId
        );

        model.addAttribute(
                "accountNo",
                accountNo
        );

        model.addAttribute(
                "accountName",
                accountName
        );

        return "qr-payment";
    }

    @PostMapping("/payment/qr/complete")
    public String qrComplete(
            @RequestParam Long orderId) {

        return "redirect:/order-success/"
                + orderId;
    }
}