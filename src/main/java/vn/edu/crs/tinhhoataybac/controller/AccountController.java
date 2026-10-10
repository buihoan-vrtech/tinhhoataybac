package vn.edu.crs.tinhhoataybac.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import vn.edu.crs.tinhhoataybac.model.Order;
import vn.edu.crs.tinhhoataybac.model.User;
import vn.edu.crs.tinhhoataybac.model.Wallet;

import vn.edu.crs.tinhhoataybac.repository.OrderServiceRequestRepository;
import vn.edu.crs.tinhhoataybac.repository.OrderStatusHistoryRepository;
import vn.edu.crs.tinhhoataybac.repository.ShipmentEventRepository;

import vn.edu.crs.tinhhoataybac.service.OrderService;
import vn.edu.crs.tinhhoataybac.service.UserService;
import vn.edu.crs.tinhhoataybac.service.WalletService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


@Controller
public class AccountController {

    private final OrderStatusHistoryRepository histories;
    private final OrderServiceRequestRepository requests;
    private final ShipmentEventRepository shipmentEvents;

    private final UserService userService;
    private final WalletService walletService;
    private final OrderService orderService;


    public AccountController(
            UserService userService,
            WalletService walletService,
            OrderService orderService,
            OrderStatusHistoryRepository histories,
            OrderServiceRequestRepository requests,
            ShipmentEventRepository shipmentEvents) {

        this.userService = userService;
        this.walletService = walletService;
        this.orderService = orderService;

        this.histories = histories;
        this.requests = requests;
        this.shipmentEvents = shipmentEvents;
    }


    /*
     * =========================================================
     * TRANG TÀI KHOẢN
     * =========================================================
     */
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
                        .getOrdersByEmail(
                                user.getEmail()
                        )
                        .stream()
                        .limit(3)
                        .toList()
        );


        return "account";
    }


    /*
     * =========================================================
     * DANH SÁCH ĐƠN HÀNG
     * =========================================================
     */
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


    /*
     * =========================================================
     * CHI TIẾT ĐƠN HÀNG
     * =========================================================
     */
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
         * Không cho khách xem đơn của tài khoản khác.
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


        /*
         * Quan trọng:
         *
         * account-order-detail.html sẽ dùng biến này
         * để quyết định có hiển thị nút
         * "HỦY ĐƠN NGAY" hay không.
         */
        model.addAttribute(
                "canCancel",
                orderService.canUserCancel(order)
        );


        /*
         * Lịch sử trạng thái đơn.
         */
        model.addAttribute(
                "histories",
                histories.findByOrderIdOrderByCreatedAtDesc(
                        orderId
                )
        );


        /*
         * Các yêu cầu hủy / hoàn tiền trước đây.
         */
        model.addAttribute(
                "requests",
                requests.findByOrderIdOrderByCreatedAtDesc(
                        orderId
                )
        );


        /*
         * Hành trình giao hàng.
         */
        model.addAttribute(
                "shipmentEvents",
                shipmentEvents
                        .findByOrderIdOrderByCreatedAtDesc(
                                orderId
                        )
        );


        return "account-order-detail";
    }


    /*
     * =========================================================
     * KHÁCH TỰ HỦY ĐƠN TRONG 1 GIỜ
     * =========================================================
     */
    @PostMapping("/account/orders/{orderId}/cancel")
    public String cancelOrder(
            @PathVariable Long orderId,
            @RequestParam(required = false)
            String reason,
            Authentication authentication) {

        User user =
                getCurrentUser(authentication);


        if (user == null) {
            return "redirect:/login";
        }


        try {

            /*
             * Service sẽ tự kiểm tra:
             *
             * - đơn có thuộc user này không
             * - còn trong 1 giờ không
             * - đã hủy chưa
             * - COD hay QR/WALLET
             * - có phải hoàn tiền hay không
             */
            orderService.cancelOrderByUser(
                    orderId,
                    user.getEmail(),
                    reason
            );


            /*
             * Thành công.
             */
            return "redirect:/account/orders/"
                    + orderId
                    + "?cancelled=true";


        } catch (Exception e) {

            /*
             * Nếu có lỗi thì gửi nội dung lỗi
             * về trang chi tiết đơn.
             */
            String message =
                    e.getMessage() == null
                            ? "Không thể hủy đơn hàng."
                            : e.getMessage();


            String encodedMessage =
                    URLEncoder.encode(
                            message,
                            StandardCharsets.UTF_8
                    );


            return "redirect:/account/orders/"
                    + orderId
                    + "?cancelError="
                    + encodedMessage;
        }
    }


    /*
     * =========================================================
     * TRANG VÍ
     * =========================================================
     */
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
                "user",
                user
        );


        model.addAttribute(
                "wallet",
                wallet
        );


        model.addAttribute(
                "transactions",
                walletService.getTransactions(
                        wallet
                )
        );


        return "wallet";
    }


    /*
     * =========================================================
     * LẤY USER ĐANG ĐĂNG NHẬP
     * =========================================================
     */
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