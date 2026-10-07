package vn.edu.crs.tinhhoataybac.model;

import jakarta.persistence.*;

@Entity
public class UserAddress {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private User user;

  private String label = "Nhà";
  private String receiverName;
  private String phone;
  private String province;
  private String district;
  private String ward;

  @Column(length = 500)
  private String addressDetail;

  private Boolean defaultAddress = false;

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

  public String getLabel() {
    return label;
  }

  public void setLabel(String value) {
    label = value;
  }

  public String getReceiverName() {
    return receiverName;
  }

  public void setReceiverName(String value) {
    receiverName = value;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String value) {
    phone = value;
  }

  public String getProvince() {
    return province;
  }

  public void setProvince(String value) {
    province = value;
  }

  public String getDistrict() {
    return district;
  }

  public void setDistrict(String value) {
    district = value;
  }

  public String getWard() {
    return ward;
  }

  public void setWard(String value) {
    ward = value;
  }

  public String getAddressDetail() {
    return addressDetail;
  }

  public void setAddressDetail(String value) {
    addressDetail = value;
  }

  public Boolean getDefaultAddress() {
    return defaultAddress;
  }

  public void setDefaultAddress(Boolean value) {
    defaultAddress = value;
  }
}
