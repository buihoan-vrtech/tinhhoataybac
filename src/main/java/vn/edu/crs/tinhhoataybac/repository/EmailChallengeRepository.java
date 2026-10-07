package vn.edu.crs.tinhhoataybac.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.edu.crs.tinhhoataybac.model.EmailChallenge;

public interface EmailChallengeRepository extends JpaRepository<EmailChallenge, Long> {
  Optional<EmailChallenge> findFirstByUserIdAndPurposeOrderByIdDesc(Long userId, String purpose);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from EmailChallenge e where e.id=:id")
  Optional<EmailChallenge> lockById(@Param("id") Long id);
}
