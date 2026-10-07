package vn.edu.crs.tinhhoataybac.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class OrderStatusHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private Order order;

  private String status;

  @Column(length = 1000)
  private String note;

  private String actor;
  private LocalDateTime createdAt = LocalDateTime.now();

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

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String value) {
    note = value;
  }

  public String getActor() {
    return actor;
  }

  public void setActor(String value) {
    actor = value;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime value) {
    createdAt = value;
  }
}
