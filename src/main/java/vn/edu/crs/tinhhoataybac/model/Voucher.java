package vn.edu.crs.tinhhoataybac.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Voucher {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String code;

  private String name;
  private String type = "fixed";

  @Column(name = "discount_value")
  private BigDecimal value = BigDecimal.ZERO;

  private BigDecimal minOrderValue = BigDecimal.ZERO;
  private BigDecimal maxDiscount;
  private Integer usageLimit;
  private Integer usedCount = 0;

  @org.springframework.format.annotation.DateTimeFormat(
      iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime startsAt;

  @org.springframework.format.annotation.DateTimeFormat(
      iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime expiresAt;

  private Boolean active = true;

  public Long getId() {
    return id;
  }

  public void setId(Long value) {
    id = value;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String value) {
    code = value;
  }

  public String getName() {
    return name;
  }

  public void setName(String value) {
    name = value;
  }

  public String getType() {
    return type;
  }

  public void setType(String value) {
    type = value;
  }

  public BigDecimal getValue() {
    return value;
  }

  public void setValue(BigDecimal value) {
    this.value = value;
  }

  public BigDecimal getMinOrderValue() {
    return minOrderValue;
  }

  public void setMinOrderValue(BigDecimal value) {
    minOrderValue = value;
  }

  public BigDecimal getMaxDiscount() {
    return maxDiscount;
  }

  public void setMaxDiscount(BigDecimal value) {
    maxDiscount = value;
  }

  public Integer getUsageLimit() {
    return usageLimit;
  }

  public void setUsageLimit(Integer value) {
    usageLimit = value;
  }

  public Integer getUsedCount() {
    return usedCount;
  }

  public void setUsedCount(Integer value) {
    usedCount = value;
  }

  public LocalDateTime getStartsAt() {
    return startsAt;
  }

  public void setStartsAt(LocalDateTime value) {
    startsAt = value;
  }

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(LocalDateTime value) {
    expiresAt = value;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean value) {
    active = value;
  }
}
