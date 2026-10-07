package vn.edu.crs.tinhhoataybac.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import vn.edu.crs.tinhhoataybac.model.Order;
import vn.edu.crs.tinhhoataybac.service.OrderService;

import java.util.Map;


@RestController
@RequestMapping("/api/payment")
public class PaymentStatusController {

    private final vn.edu.crs.tinhhoataybac.service.CommerceService commerce;
    private final vn.edu.crs.tinhhoataybac.service.UserService users;
    private final OrderService orderService;


    public PaymentStatusController(
            OrderService orderService,
            vn.edu.crs.tinhhoataybac.service.CommerceService commerce,
            vn.edu.crs.tinhhoataybac.service.UserService users) {
        this.commerce=commerce;this.users=users;

        this.orderService = orderService;
    }


    @GetMapping("/status/{orderId}")
    public ResponseEntity<?> status(
            @PathVariable Long orderId, org.springframework.security.core.Authentication authentication) {

        Order order =
                orderService.getOrderById(orderId);


        if (authentication==null || order == null || !commerce.owns(order,users.findByEmail(authentication.getName()))) {

            return ResponseEntity
                    .notFound()
                    .build();
        }


        boolean paid =
                "PAID".equalsIgnoreCase(
                        order.getPaymentStatus()
                );


        return ResponseEntity.ok(

                Map.of(
                        "orderId",
                        order.getId(),

                        "paymentStatus",
                        order.getPaymentStatus(),

                        "paid",
                        paid
                )
        );
    }
}