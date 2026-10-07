package vn.edu.crs.tinhhoataybac.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class OrderServiceRequest {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private Order order;

  @ManyToOne(optional = false)
  private User user;

  private String type;
  private String status = "PENDING";

  @Column(length = 1000)
  private String reason;

  @Column(length = 1000)
  private String adminNote;

  private LocalDateTime createdAt = LocalDateTime.now();
  private LocalDateTime processedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long value) {
    id = value;
  }

  public Order getOrder() {
    return order;
  }

  public void setOrder(Order value) {
    order = value;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User value) {
    user = value;
  }

  public String getType() {
    return type;
  }

  public void setType(String value) {
    type = value;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String value) {
    reason = value;
  }

  public String getAdminNote() {
    return adminNote;
  }

  public void setAdminNote(String value) {
    adminNote = value;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime value) {
    createdAt = value;
  }

  public LocalDateTime getProcessedAt() {
    return processedAt;
  }

  public void setProcessedAt(LocalDateTime value) {
    processedAt = value;
  }
}
