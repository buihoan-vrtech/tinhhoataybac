package vn.edu.crs.tinhhoataybac.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class CustomerNotification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private User user;

  @Column(length = 1000)
  private String message;

  private Long orderId;
  private Boolean readFlag = false;
  private LocalDateTime createdAt = LocalDateTime.now();

  public Long getId() {
    return id;
  }

  public void setId(Long value) {
    id = value;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User value) {
    user = value;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String value) {
    message = value;
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long value) {
    orderId = value;
  }

  public Boolean getReadFlag() {
    return readFlag;
  }

  public void setReadFlag(Boolean value) {
    readFlag = value;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime value) {
    createdAt = value;
  }
}
