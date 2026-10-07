package vn.edu.crs.tinhhoataybac.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.edu.crs.tinhhoataybac.model.OrderServiceRequest;

public interface OrderServiceRequestRepository extends JpaRepository<OrderServiceRequest, Long> {
  List<OrderServiceRequest> findByOrderIdOrderByCreatedAtDesc(Long orderId);

  List<OrderServiceRequest> findAllByOrderByCreatedAtDesc();

  boolean existsByOrderIdAndStatus(Long orderId, String status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from OrderServiceRequest r where r.id=:id")
  Optional<OrderServiceRequest> lockById(@Param("id") Long id);
}
