package vn.edu.crs.tinhhoataybac.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.crs.tinhhoataybac.model.WalletTransaction;

import java.util.List;

public interface WalletTransactionRepository
        extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction>
    findByWalletIdOrderByCreatedAtDesc(Long walletId);
}