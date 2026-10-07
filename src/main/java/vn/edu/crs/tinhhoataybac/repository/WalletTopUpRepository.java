package vn.edu.crs.tinhhoataybac.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.edu.crs.tinhhoataybac.model.WalletTopUp;

import java.util.List;
import java.util.Optional;


public interface WalletTopUpRepository
        extends JpaRepository<WalletTopUp, Long> {


    Optional<WalletTopUp>
    findByPaymentCode(String paymentCode);


    List<WalletTopUp>
    findByUserIdOrderByCreatedAtDesc(Long userId);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select t from WalletTopUp t where t.paymentCode=:code")
 Optional<WalletTopUp> lockByCode(@org.springframework.data.repository.query.Param("code")String code);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select t from WalletTopUp t where t.id=:id")
 Optional<WalletTopUp> lockById(@org.springframework.data.repository.query.Param("id")Long id);
}