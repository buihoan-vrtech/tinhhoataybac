package vn.edu.crs.tinhhoataybac.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import vn.edu.crs.tinhhoataybac.model.ShipmentEvent;

public interface ShipmentEventRepository extends JpaRepository<ShipmentEvent, Long> {
  List<ShipmentEvent> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
