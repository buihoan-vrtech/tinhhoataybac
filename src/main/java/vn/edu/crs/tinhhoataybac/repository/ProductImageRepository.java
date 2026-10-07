package vn.edu.crs.tinhhoataybac.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import vn.edu.crs.tinhhoataybac.model.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
  List<ProductImage> findByProductIdOrderByIdAsc(Long id);
}
