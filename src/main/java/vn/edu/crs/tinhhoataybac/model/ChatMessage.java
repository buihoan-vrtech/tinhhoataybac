package vn.edu.crs.tinhhoataybac.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="shop_chat_messages",indexes=@Index(columnList="customer_id,id"))
public class ChatMessage {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false) @JoinColumn(name="customer_id",nullable=false) private User customer;
 @ManyToOne(optional=false) private User sender;
 @Column(nullable=false,length=1200) private String content;
 @Column(nullable=false) private boolean adminSent;
 @Column(nullable=false) private boolean readByRecipient;
 @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
 public Long getId(){return id;} public User getCustomer(){return customer;} public void setCustomer(User u){customer=u;} public User getSender(){return sender;} public void setSender(User u){sender=u;} public String getContent(){return content;} public void setContent(String v){content=v;} public boolean isAdminSent(){return adminSent;} public void setAdminSent(boolean v){adminSent=v;} public boolean isReadByRecipient(){return readByRecipient;} public LocalDateTime getCreatedAt(){return createdAt;}
}
