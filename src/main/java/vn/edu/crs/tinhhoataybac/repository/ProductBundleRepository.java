package vn.edu.crs.tinhhoataybac.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.crs.tinhhoataybac.model.ProductBundle;
public interface ProductBundleRepository extends JpaRepository<ProductBundle,Long>{java.util.List<ProductBundle> findByActiveTrueOrderByIdAsc();}
