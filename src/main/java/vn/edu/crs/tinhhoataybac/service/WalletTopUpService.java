package vn.edu.crs.tinhhoataybac.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.crs.tinhhoataybac.model.User;
import vn.edu.crs.tinhhoataybac.model.Wallet;
import vn.edu.crs.tinhhoataybac.model.WalletTopUp;

import vn.edu.crs.tinhhoataybac.repository.WalletTopUpRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Service
public class WalletTopUpService {

    private final NotificationService notifications;
    private final WalletTopUpRepository walletTopUpRepository;

    private final WalletService walletService;


    public WalletTopUpService(
            WalletTopUpRepository walletTopUpRepository,
            WalletService walletService,NotificationService notifications) {
        this.notifications=notifications;

        this.walletTopUpRepository =
                walletTopUpRepository;

        this.walletService =
                walletService;
    }


    /*
     * ==========================================
     * TẠO YÊU CẦU NẠP TIỀN
     * ==========================================
     */
    @Transactional
    public WalletTopUp createTopUp(
            User user,
            BigDecimal amount) {


        if (user == null) {

            throw new IllegalArgumentException(
                    "Người dùng không hợp lệ."
            );
        }


        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Số tiền nạp phải lớn hơn 0."
            );
        }


        /*
         * Có thể giới hạn tối thiểu.
         *
         * Ví dụ:
         * ít nhất 10.000đ
         */
        if (amount.compareTo(
                new BigDecimal("10000")
        ) < 0) {

            throw new IllegalArgumentException(
                    "Số tiền nạp tối thiểu là 10.000đ."
            );
        }


        Wallet wallet =
                walletService.getOrCreateWallet(user);


        WalletTopUp topUp =
                new WalletTopUp();


        topUp.setUser(user);

        topUp.setWallet(wallet);

        topUp.setAmount(amount);

        topUp.setStatus("PENDING");


        /*
         * Lưu lần đầu để lấy ID.
         */
        WalletTopUp savedTopUp =
                walletTopUpRepository.save(topUp);


        /*
         * Sinh mã:
         *
         * ID 1 -> NAP1
         * ID 2 -> NAP2
         * ID 3 -> NAP3
         */
        savedTopUp.setPaymentCode(
                "NAP" + savedTopUp.getId()
        );


        return walletTopUpRepository.save(
                savedTopUp
        );
    }


    /*
     * ==========================================
     * LẤY YÊU CẦU THEO ID
     * ==========================================
     */
    public WalletTopUp getById(
            Long id) {

        if (id == null) {
            return null;
        }


        return walletTopUpRepository
                .findById(id)
                .orElse(null);
    }


    /*
     * ==========================================
     * LẤY THEO MÃ NAP
     * ==========================================
     */
    public WalletTopUp getByPaymentCode(
            String paymentCode) {

        if (paymentCode == null
                || paymentCode.isBlank()) {

            return null;
        }


        return walletTopUpRepository
                .findByPaymentCode(
                        paymentCode.trim()
                                .toUpperCase()
                )
                .orElse(null);
    }


    /*
     * ==========================================
     * LỊCH SỬ YÊU CẦU NẠP CỦA USER
     * ==========================================
     */
    public List<WalletTopUp> getTopUpsByUser(
            User user) {

        if (user == null
                || user.getId() == null) {

            return List.of();
        }


        return walletTopUpRepository
                .findByUserIdOrderByCreatedAtDesc(
                        user.getId()
                );
    }


    /*
     * ==========================================
     * XÁC NHẬN NẠP TIỀN THÀNH CÔNG
     * ==========================================
     *
     * Hàm này sẽ được SePay IPN gọi ở bước sau.
     */
    @Transactional
    public boolean confirmTopUp(
            String paymentCode,
            BigDecimal receivedAmount) {


        WalletTopUp topUp =
                walletTopUpRepository.lockByCode(paymentCode.trim().toUpperCase(java.util.Locale.ROOT)).orElse(null);


        if (topUp == null) {

            return false;
        }


        /*
         * Nếu đã nạp rồi thì không cộng lần nữa.
         */
        if ("PAID".equalsIgnoreCase(
                topUp.getStatus()
        )) {

            return true;
        }


        if(!"PENDING".equalsIgnoreCase(topUp.getStatus()))return false;
        if (receivedAmount == null
                || receivedAmount.compareTo(
                topUp.getAmount()
        ) < 0) {

            return false;
        }


        /*
         * Cộng tiền vào ví.
         */
        walletService.deposit(
                topUp.getUser(),
                topUp.getAmount(),
                "Nạp tiền vào Ví Tinh Hoa",
                topUp.getPaymentCode()
        );


        /*
         * Đánh dấu yêu cầu nạp đã thanh toán.
         */
        topUp.setStatus("PAID");
        notifications.notify(topUp.getUser(),null,"Đã nạp "+topUp.getAmount()+" đ vào Ví Tinh Hoa.");

        topUp.setPaidAt(
                LocalDateTime.now()
        );


        walletTopUpRepository.save(topUp);


        return true;
    }
}