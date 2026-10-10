package vn.edu.crs.tinhhoataybac.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.crs.tinhhoataybac.model.GiftWrapOption;
public interface GiftWrapOptionRepository extends JpaRepository<GiftWrapOption,Long>{java.util.List<GiftWrapOption> findByActiveTrueOrderByIdAsc();}
