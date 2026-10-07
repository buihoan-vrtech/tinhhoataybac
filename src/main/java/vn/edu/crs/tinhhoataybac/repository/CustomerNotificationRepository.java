package vn.edu.crs.tinhhoataybac.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import vn.edu.crs.tinhhoataybac.model.CustomerNotification;

public interface CustomerNotificationRepository extends JpaRepository<CustomerNotification, Long> {
  List<CustomerNotification> findByUserIdOrderByCreatedAtDesc(Long userId);

  Optional<CustomerNotification> findByIdAndUserId(Long id, Long userId);
}
