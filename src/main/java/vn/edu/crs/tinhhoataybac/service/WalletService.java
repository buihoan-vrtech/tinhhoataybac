package vn.edu.crs.tinhhoataybac.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.crs.tinhhoataybac.model.User;
import vn.edu.crs.tinhhoataybac.model.Wallet;
import vn.edu.crs.tinhhoataybac.model.WalletTransaction;

import vn.edu.crs.tinhhoataybac.repository.WalletRepository;
import vn.edu.crs.tinhhoataybac.repository.WalletTransactionRepository;

import java.math.BigDecimal;
import java.util.List;


@Service
public class WalletService {

    private final vn.edu.crs.tinhhoataybac.repository.UserRepository users;
    private final WalletRepository walletRepository;

    private final WalletTransactionRepository
            walletTransactionRepository;


    public WalletService(
            WalletRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository,
            vn.edu.crs.tinhhoataybac.repository.UserRepository users) {
        this.users=users;

        this.walletRepository =
                walletRepository;

        this.walletTransactionRepository =
                walletTransactionRepository;
    }


    /*
     * =========================================================
     * LẤY HOẶC TẠO VÍ
     * =========================================================
     */
    @Transactional
    public Wallet getOrCreateWallet(User user) {

        if (user == null
                || user.getId() == null) {

            throw new IllegalArgumentException(
                    "Người dùng không hợp lệ."
            );
        }


        users.lockById(user.getId()).orElseThrow();
        return walletRepository
                .findByUserId(user.getId())
                .orElseGet(() -> {

                    Wallet wallet =
                            new Wallet();

                    wallet.setUser(user);

                    wallet.setBalance(
                            BigDecimal.ZERO
                    );


                    return walletRepository.save(
                            wallet
                    );
                });
    }


    /*
     * =========================================================
     * LẤY LỊCH SỬ GIAO DỊCH
     * =========================================================
     */
    public List<WalletTransaction> getTransactions(
            Wallet wallet) {

        if (wallet == null
                || wallet.getId() == null) {

            return List.of();
        }


        return walletTransactionRepository
                .findByWalletIdOrderByCreatedAtDesc(
                        wallet.getId()
                );
    }


    /*
     * =========================================================
     * LẤY SỐ DƯ
     * =========================================================
     */
    @Transactional
    public BigDecimal getBalance(
            User user) {

        Wallet wallet =
                getOrCreateWallet(user);


        if (wallet.getBalance() == null) {

            return BigDecimal.ZERO;
        }


        return wallet.getBalance();
    }


    /*
     * =========================================================
     * KIỂM TRA ĐỦ TIỀN KHÔNG
     * =========================================================
     */
    public boolean hasEnoughBalance(
            User user,
            BigDecimal amount) {

        if (user == null
                || amount == null
                || amount.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            return false;
        }


        BigDecimal balance =
                getBalance(user);


        return balance.compareTo(
                amount
        ) >= 0;
    }


    /*
     * =========================================================
     * NẠP TIỀN
     * =========================================================
     */
    @Transactional
    public void deposit(
            User user,
            BigDecimal amount,
            String description,
            String referenceCode) {

        if (amount == null
                || amount.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new IllegalArgumentException(
                    "Số tiền nạp không hợp lệ."
            );
        }


        Wallet wallet =
                getOrCreateWallet(user);


        BigDecimal currentBalance =
                wallet.getBalance() == null
                        ? BigDecimal.ZERO
                        : wallet.getBalance();


        BigDecimal newBalance =
                currentBalance.add(amount);


        wallet.setBalance(
                newBalance
        );


        walletRepository.save(
                wallet
        );


        /*
         * Lưu lịch sử giao dịch.
         */
        WalletTransaction transaction =
                new WalletTransaction();


        transaction.setWallet(
                wallet
        );


        transaction.setType(
                "DEPOSIT"
        );


        /*
         * Nạp tiền:
         * amount là số dương.
         */
        transaction.setAmount(
                amount
        );


        transaction.setBalanceAfter(
                newBalance
        );


        transaction.setDescription(
                description
        );


        transaction.setReferenceCode(
                referenceCode
        );


        transaction.setStatus(
                "SUCCESS"
        );


        walletTransactionRepository.save(
                transaction
        );
    }


    /*
     * =========================================================
     * THANH TOÁN BẰNG VÍ
     * =========================================================
     */
    @Transactional
    public void pay(
            User user,
            BigDecimal amount,
            String description,
            String referenceCode) {

        /*
         * Kiểm tra tiền thanh toán.
         */
        if (amount == null
                || amount.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new IllegalArgumentException(
                    "Số tiền thanh toán không hợp lệ."
            );
        }


        Wallet wallet =
                getOrCreateWallet(user);


        BigDecimal currentBalance =
                wallet.getBalance() == null
                        ? BigDecimal.ZERO
                        : wallet.getBalance();


        /*
         * Không đủ tiền.
         */
        if (currentBalance.compareTo(
                amount
        ) < 0) {

            throw new IllegalStateException(
                    "Số dư Ví Tinh Hoa không đủ."
            );
        }


        /*
         * Trừ tiền.
         */
        BigDecimal newBalance =
                currentBalance.subtract(
                        amount
                );


        wallet.setBalance(
                newBalance
        );


        walletRepository.save(
                wallet
        );


        /*
         * Tạo giao dịch PAYMENT.
         */
        WalletTransaction transaction =
                new WalletTransaction();


        transaction.setWallet(
                wallet
        );


        transaction.setType(
                "PAYMENT"
        );


        /*
         * Thanh toán:
         *
         * lưu amount âm để lịch sử hiển thị:
         *
         * -450.000đ
         */
        transaction.setAmount(
                amount.negate()
        );


        transaction.setBalanceAfter(
                newBalance
        );


        transaction.setDescription(
                description
        );


        transaction.setReferenceCode(
                referenceCode
        );


        transaction.setStatus(
                "SUCCESS"
        );


        walletTransactionRepository.save(
                transaction
        );
    }
    @Transactional
    public void refund(
            User user,
            BigDecimal amount,
            String description,
            String referenceCode) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "Người dùng không hợp lệ."
            );
        }

        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Số tiền hoàn không hợp lệ."
            );
        }

        Wallet wallet =
                getOrCreateWallet(user);

        BigDecimal currentBalance =
                wallet.getBalance() == null
                        ? BigDecimal.ZERO
                        : wallet.getBalance();

        BigDecimal newBalance =
                currentBalance.add(amount);

        wallet.setBalance(newBalance);

        walletRepository.save(wallet);


        WalletTransaction transaction =
                new WalletTransaction();

        transaction.setWallet(wallet);

        transaction.setType("REFUND");

        transaction.setAmount(amount);

        transaction.setBalanceAfter(newBalance);

        transaction.setDescription(description);

        transaction.setReferenceCode(referenceCode);

        transaction.setStatus("SUCCESS");

        walletTransactionRepository.save(transaction);
    }
}