package vn.edu.crs.tinhhoataybac.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class EmailChallenge {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private User user;

  private String purpose;
  private String codeHash;
  private LocalDateTime expiresAt;
  private LocalDateTime sentAt;
  private Integer attempts = 0;
  private Boolean consumed = false;

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

  public String getPurpose() {
    return purpose;
  }

  public void setPurpose(String value) {
    purpose = value;
  }

  public String getCodeHash() {
    return codeHash;
  }

  public void setCodeHash(String value) {
    codeHash = value;
  }

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(LocalDateTime value) {
    expiresAt = value;
  }

  public LocalDateTime getSentAt() {
    return sentAt;
  }

  public void setSentAt(LocalDateTime value) {
    sentAt = value;
  }

  public Integer getAttempts() {
    return attempts;
  }

  public void setAttempts(Integer value) {
    attempts = value;
  }

  public Boolean getConsumed() {
    return consumed;
  }

  public void setConsumed(Boolean value) {
    consumed = value;
  }
}
