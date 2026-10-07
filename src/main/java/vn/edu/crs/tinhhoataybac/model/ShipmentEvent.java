package vn.edu.crs.tinhhoataybac.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ShipmentEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private Order order;

  private String status;
  private String carrier;
  private String trackingCode;
  private String location;

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

  public String getCarrier() {
    return carrier;
  }

  public void setCarrier(String value) {
    carrier = value;
  }

  public String getTrackingCode() {
    return trackingCode;
  }

  public void setTrackingCode(String value) {
    trackingCode = value;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String value) {
    location = value;
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
