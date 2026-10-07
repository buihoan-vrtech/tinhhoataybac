package vn.edu.crs.tinhhoataybac.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.crs.tinhhoataybac.model.Wallet;

import java.util.Optional;

public interface WalletRepository
        extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(Long userId);
}