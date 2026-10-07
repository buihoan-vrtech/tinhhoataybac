package vn.edu.crs.tinhhoataybac.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import vn.edu.crs.tinhhoataybac.model.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {
  List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

  Optional<Review> findByUserIdAndProductId(Long userId, Long productId);
}
