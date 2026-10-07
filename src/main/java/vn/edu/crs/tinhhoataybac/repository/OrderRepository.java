package vn.edu.crs.tinhhoataybac.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.crs.tinhhoataybac.model.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository
        extends JpaRepository<Order, Long> {

    Optional<Order> findByPaymentCode(String paymentCode);

    List<Order> findByEmailIgnoreCaseOrderByCreatedAtDesc(
            String email
    );

    Optional<Order> findByIdAndEmailIgnoreCase(
            Long id,
            String email
    );

 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select o from Order o where o.id=:id")
 Optional<Order> lockById(@org.springframework.data.repository.query.Param("id") Long id);
 List<Order> findByCustomerIdOrderByCreatedAtDesc(Long id);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select o from Order o where o.paymentCode=:code")
 Optional<Order> lockByPaymentCode(@org.springframework.data.repository.query.Param("code")String code);
}