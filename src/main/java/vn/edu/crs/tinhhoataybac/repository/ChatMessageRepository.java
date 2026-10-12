package vn.edu.crs.tinhhoataybac.repository;
import vn.edu.crs.tinhhoataybac.model.ChatMessage;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface ChatMessageRepository extends JpaRepository<ChatMessage,Long>{
 List<ChatMessage> findByCustomerIdOrderByIdDesc(Long id,Pageable page);
 long countByCustomerIdAndAdminSentAndReadByRecipientFalse(Long id,boolean adminSent);
 long countBySenderIdAndCreatedAtAfter(Long id,java.time.LocalDateTime since);
 @Query("select m.customer.id from ChatMessage m group by m.customer.id order by max(m.id) desc") List<Long> inboxIds(Pageable page);
 @Modifying @Query("update ChatMessage m set m.readByRecipient=true where m.customer.id=:id and m.adminSent=:adminSent and m.id<=:through") int markRead(@Param("id")Long id,@Param("adminSent")boolean adminSent,@Param("through")Long through);
}
