package vn.edu.crs.tinhhoataybac.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.crs.tinhhoataybac.model.SePayTransaction;

public interface SePayTransactionRepository
        extends JpaRepository<SePayTransaction, Long> {

    boolean existsBySepayTransactionId(Long sepayTransactionId);
}