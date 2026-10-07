package vn.edu.crs.tinhhoataybac.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import vn.edu.crs.tinhhoataybac.model.User;
import vn.edu.crs.tinhhoataybac.model.WalletTopUp;

import vn.edu.crs.tinhhoataybac.service.SePayService;
import vn.edu.crs.tinhhoataybac.service.UserService;
import vn.edu.crs.tinhhoataybac.service.WalletTopUpService;

import java.math.BigDecimal;


@Controller
@RequestMapping("/wallet/topup")
public class WalletTopUpController {

    private final UserService userService;

    private final WalletTopUpService walletTopUpService;

    private final SePayService sePayService;


    public WalletTopUpController(
            UserService userService,
            WalletTopUpService walletTopUpService,
            SePayService sePayService) {

        this.userService = userService;

        this.walletTopUpService = walletTopUpService;

        this.sePayService = sePayService;
    }


    /*
     * =========================================================
     * TRANG NHẬP SỐ TIỀN NẠP
     * =========================================================
     */
    @GetMapping
    public String topUpPage(
            Authentication authentication,
            Model model) {

        User user = getCurrentUser(authentication);


        if (user == null) {

            return "redirect:/login";
        }


        model.addAttribute(
                "user",
                user
        );


        return "wallet-topup";
    }


    /*
     * =========================================================
     * TẠO YÊU CẦU NẠP TIỀN
     * =========================================================
     */
    @PostMapping
    public String createTopUp(
            @RequestParam String amount,
            Authentication authentication,
            Model model) {

        User user = getCurrentUser(authentication);


        if (user == null) {

            return "redirect:/login";
        }


        try {

            /*
             * Xóa dấu chấm, dấu phẩy và khoảng trắng.
             *
             * Ví dụ:
             *
             * 100.000
             * 100,000
             *
             * đều thành:
             *
             * 100000
             */
            String cleanAmount =
                    amount
                            .replace(".", "")
                            .replace(",", "")
                            .replace(" ", "")
                            .trim();


            BigDecimal money =
                    new BigDecimal(cleanAmount);


            /*
             * Tạo yêu cầu nạp tiền.
             *
             * Ví dụ:
             *
             * NAP1
             * NAP2
             * NAP3
             */
            WalletTopUp topUp =
                    walletTopUpService.createTopUp(
                            user,
                            money
                    );


            /*
             * Chuyển tới trang xác nhận
             * trước khi sang SePay.
             */
            return "redirect:/wallet/topup/pay/"
                    + topUp.getId();


        } catch (Exception e) {

            model.addAttribute(
                    "user",
                    user
            );


            model.addAttribute(
                    "error",
                    e.getMessage()
            );


            model.addAttribute(
                    "amount",
                    amount
            );


            return "wallet-topup";
        }
    }


    /*
     * =========================================================
     * TRANG XÁC NHẬN THANH TOÁN
     * =========================================================
     */
    @GetMapping("/pay/{id}")
    public String pay(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {

        User user = getCurrentUser(authentication);


        if (user == null) {

            return "redirect:/login";
        }


        /*
         * Tìm yêu cầu nạp tiền.
         */
        WalletTopUp topUp =
                walletTopUpService.getById(id);


        /*
         * Không tồn tại yêu cầu nạp.
         */
        if (topUp == null) {

            return "redirect:/wallet/topup";
        }


        /*
         * =====================================================
         * BẢO MẬT:
         * Không cho user A xem yêu cầu nạp của user B.
         * =====================================================
         */
        if (topUp.getUser() == null
                || topUp.getUser().getId() == null
                || !topUp.getUser()
                .getId()
                .equals(user.getId())) {

            return "redirect:/wallet";
        }


        /*
         * Nếu giao dịch đã PAID thì
         * không cho thanh toán lại.
         */
        if ("PAID".equalsIgnoreCase(
                topUp.getStatus()
        )) {

            return "redirect:/wallet";
        }


        /*
         * Đưa dữ liệu yêu cầu nạp
         * sang HTML.
         */
        model.addAttribute(
                "topUp",
                topUp
        );


        /*
         * URL SePay Sandbox.
         */
        model.addAttribute(
                "checkoutUrl",
                sePayService.getCheckoutUrl()
        );


        /*
         * Quan trọng:
         *
         * Dùng chính SePayService đang chạy
         * thành công cho đơn hàng.
         *
         * Không dùng WalletTopUpPaymentService nữa.
         */
        model.addAttribute(
                "sepayFields",
                sePayService
                        .buildWalletTopUpFields(topUp)
        );


        return "wallet-topup-checkout";
    }


    /*
     * =========================================================
     * SEPAY TRẢ VỀ KHI THANH TOÁN THÀNH CÔNG
     * =========================================================
     *
     * LƯU Ý:
     *
     * Không cộng tiền tại đây.
     *
     * Việc cộng tiền phải do IPN từ SePay xử lý.
     *
     * Điều này tránh trường hợp người dùng
     * tự mở URL success để cộng tiền giả.
     * =========================================================
     */
    @GetMapping("/success/{id}")
    public String success(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getCurrentUser(authentication);


        if (user == null) {

            return "redirect:/login";
        }


        WalletTopUp topUp =
                walletTopUpService.getById(id);


        if (topUp == null) {

            return "redirect:/wallet";
        }


        /*
         * Kiểm tra giao dịch có thuộc user không.
         */
        if (topUp.getUser() == null
                || !topUp.getUser()
                .getId()
                .equals(user.getId())) {

            return "redirect:/wallet";
        }


        /*
         * Nếu IPN đã về và giao dịch PAID
         * thì về ví luôn.
         */
        if ("PAID".equalsIgnoreCase(
                topUp.getStatus()
        )) {

            return "redirect:/wallet?topup=success";
        }


        /*
         * Nếu SePay redirect về trước
         * nhưng IPN chưa tới thì báo processing.
         */
        return "redirect:/wallet?topup=processing";
    }


    /*
     * =========================================================
     * SEPAY TRẢ VỀ KHI THANH TOÁN LỖI
     * =========================================================
     */
    @GetMapping("/error/{id}")
    public String error(
            @PathVariable Long id) {

        return "redirect:/wallet/topup?paymentError";
    }


    /*
     * =========================================================
     * NGƯỜI DÙNG HỦY THANH TOÁN
     * =========================================================
     */
    @GetMapping("/cancel/{id}")
    public String cancel(
            @PathVariable Long id) {

        return "redirect:/wallet/topup?cancelled";
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