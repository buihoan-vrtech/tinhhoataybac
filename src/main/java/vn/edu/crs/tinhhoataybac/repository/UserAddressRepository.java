package vn.edu.crs.tinhhoataybac.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import vn.edu.crs.tinhhoataybac.model.UserAddress;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
  List<UserAddress> findByUserIdOrderByDefaultAddressDescIdDesc(Long userId);

  Optional<UserAddress> findByIdAndUserId(Long id, Long userId);
}
