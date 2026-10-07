package vn.edu.crs.tinhhoataybac.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import vn.edu.crs.tinhhoataybac.model.Order;
import vn.edu.crs.tinhhoataybac.model.User;
import vn.edu.crs.tinhhoataybac.model.Wallet;

import vn.edu.crs.tinhhoataybac.service.OrderService;
import vn.edu.crs.tinhhoataybac.service.UserService;
import vn.edu.crs.tinhhoataybac.service.WalletService;


@Controller
public class AccountController {

    private final vn.edu.crs.tinhhoataybac.repository.OrderStatusHistoryRepository histories;
    private final vn.edu.crs.tinhhoataybac.repository.OrderServiceRequestRepository requests;
    private final vn.edu.crs.tinhhoataybac.repository.ShipmentEventRepository shipmentEvents;
    private final UserService userService;

    private final WalletService walletService;

    private final OrderService orderService;


    public AccountController(
            UserService userService,
            WalletService walletService,
            OrderService orderService,
            vn.edu.crs.tinhhoataybac.repository.OrderStatusHistoryRepository histories,
            vn.edu.crs.tinhhoataybac.repository.OrderServiceRequestRepository requests, vn.edu.crs.tinhhoataybac.repository.ShipmentEventRepository shipmentEvents) {
        this.shipmentEvents=shipmentEvents;
        this.histories=histories;this.requests=requests;

        this.userService = userService;

        this.walletService = walletService;

        this.orderService = orderService;
    }


    @GetMapping("/account")
    public String account(
            Authentication authentication,
            Model model) {

        User user =
                getCurrentUser(authentication);

        if (user == null) {
            return "redirect:/login";
        }


        Wallet wallet =
                walletService.getOrCreateWallet(user);


        model.addAttribute(
                "user",
                user
        );

        model.addAttribute(
                "wallet",
                wallet
        );

        model.addAttribute(
                "recentOrders",
                orderService
                        .getOrdersByEmail(user.getEmail())
                        .stream()
                        .limit(3)
                        .toList()
        );


        return "account";
    }


    @GetMapping("/account/orders")
    public String orders(
            Authentication authentication,
            Model model) {

        User user =
                getCurrentUser(authentication);

        if (user == null) {
            return "redirect:/login";
        }


        model.addAttribute(
                "user",
                user
        );

        model.addAttribute(
                "orders",
                orderService.getOrdersByEmail(
                        user.getEmail()
                )
        );


        return "account-orders";
    }


    @GetMapping("/account/orders/{orderId}")
    public String orderDetail(
            @PathVariable Long orderId,
            Authentication authentication,
            Model model) {

        User user =
                getCurrentUser(authentication);

        if (user == null) {
            return "redirect:/login";
        }


        Order order =
                orderService.getOrderForUser(
                        orderId,
                        user.getEmail()
                );


        /*
         * Không cho xem đơn của người khác.
         */
        if (order == null) {

            return "redirect:/account/orders";
        }


        model.addAttribute(
                "user",
                user
        );

        model.addAttribute(
                "order",
                order
        );


        model.addAttribute("histories",histories.findByOrderIdOrderByCreatedAtDesc(orderId));
        model.addAttribute("requests",requests.findByOrderIdOrderByCreatedAtDesc(orderId));
        model.addAttribute("shipmentEvents",shipmentEvents.findByOrderIdOrderByCreatedAtDesc(orderId));return "account-order-detail";
    }


    @GetMapping("/wallet")
    public String wallet(
            Authentication authentication,
            Model model) {

        User user =
                getCurrentUser(authentication);

        if (user == null) {
            return "redirect:/login";
        }


        Wallet wallet =
                walletService.getOrCreateWallet(user);


        model.addAttribute(
                "wallet",
                wallet
        );


        model.addAttribute(
                "transactions",
                walletService.getTransactions(wallet)
        );


        return "wallet";
    }


    private User getCurrentUser(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {

            return null;
        }


        return userService.findByEmail(
                authentication.getName()
        );
    }
}