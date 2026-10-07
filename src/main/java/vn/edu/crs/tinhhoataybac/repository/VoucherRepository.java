package vn.edu.crs.tinhhoataybac.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.edu.crs.tinhhoataybac.model.Voucher;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
  Optional<Voucher> findByCodeIgnoreCase(String code);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select v from Voucher v where upper(v.code)=upper(:code)")
  Optional<Voucher> lockByCode(@Param("code") String code);
}
