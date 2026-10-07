package vn.edu.crs.tinhhoataybac.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import vn.edu.crs.tinhhoataybac.model.OrderStatusHistory;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
  List<OrderStatusHistory> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
