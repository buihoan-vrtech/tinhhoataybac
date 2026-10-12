package vn.edu.crs.tinhhoataybac.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import vn.edu.crs.tinhhoataybac.model.*;
import vn.edu.crs.tinhhoataybac.repository.*;
import java.util.*;
@Service public class ShopChatService {
 private final UserRepository users;private final ChatMessageRepository messages;
 public ShopChatService(UserRepository users,ChatMessageRepository messages){this.users=users;this.messages=messages;}
 private User customer(Long id){var u=users.findById(id).orElseThrow(()->new IllegalStateException("Không tìm thấy khách hàng."));if(!"USER".equals(u.getRole()))throw new IllegalStateException("Hội thoại cần tài khoản khách hàng.");return u;}
 private Map<String,Object> item(ChatMessage m){return Map.of("id",m.getId(),"content",m.getContent(),"adminSent",m.isAdminSent(),"createdAt",m.getCreatedAt().toString(),"read",m.isReadByRecipient());}
 @Transactional(readOnly=true) public Map<String,Object> thread(Long id,boolean admin){customer(id);var list=new ArrayList<>(messages.findByCustomerIdOrderByIdDesc(id,PageRequest.of(0,100)));Collections.reverse(list);return Map.of("messages",list.stream().map(this::item).toList(),"unread",messages.countByCustomerIdAndAdminSentAndReadByRecipientFalse(id,!admin));}
 @Transactional public void send(Long customerId,User actor,boolean admin,String content){if(content==null||content.isBlank()||content.length()>1200)throw new IllegalStateException("Tin nhắn cần từ 1 đến 1200 ký tự.");User recipient=users.lockById(customerId).orElseThrow();customer(recipient.getId());if(actor==null||(!admin&&!Objects.equals(actor.getId(),customerId))||(admin&&!"ADMIN".equals(actor.getRole())))throw new IllegalStateException("Bạn không có quyền gửi tin nhắn.");if(messages.countBySenderIdAndCreatedAtAfter(actor.getId(),java.time.LocalDateTime.now().minusMinutes(1))>=15)throw new IllegalStateException("Bạn gửi quá nhanh. Vui lòng đợi một phút.");var m=new ChatMessage();m.setCustomer(recipient);m.setSender(actor);m.setAdminSent(admin);m.setContent(content.trim());messages.save(m);}
 @Transactional public void read(Long id,boolean admin,Long through){customer(id);if(through!=null&&through>0)messages.markRead(id,!admin,through);}
 @Transactional(readOnly=true) public List<Map<String,Object>> inbox(){return messages.inboxIds(PageRequest.of(0,100)).stream().map(id->{var u=customer(id);var latest=messages.findByCustomerIdOrderByIdDesc(id,PageRequest.of(0,1)).get(0);return Map.<String,Object>of("customerId",id,"name",u.getFullName(),"preview",latest.getContent(),"unread",messages.countByCustomerIdAndAdminSentAndReadByRecipientFalse(id,false));}).toList();}
}
