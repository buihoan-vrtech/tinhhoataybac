package vn.edu.crs.tinhhoataybac.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;

    private LocalDateTime cancelledAt;

    private Boolean refunded;

    private LocalDateTime refundedAt;

    @Column(length = 500)
    private String cancelReason;

    @Column(nullable = false)
    private String phone;

    private String email;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(length = 1000)
    private String note;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String paymentMethod;

    @Column(nullable = false)
    private String paymentStatus;

    @Column(nullable = false)
    private String orderStatus;

    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    @Column(unique = true)
    private String paymentCode;

    @OneToMany(mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<OrderDetail> orderDetails = new ArrayList<>();

    public Order() {
    }

    @PrePersist
    public void prePersist() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (paymentStatus == null) {
            paymentStatus = "UNPAID";
        }

        if (orderStatus == null) {
            orderStatus = "PENDING";
        }
    }

    public void addOrderDetail(OrderDetail detail) {

        orderDetails.add(detail);

        detail.setOrder(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getPaymentCode() {
        return paymentCode;
    }

    public void setPaymentCode(String paymentCode) {
        this.paymentCode = paymentCode;
    }

    public List<OrderDetail> getOrderDetails() {
        return orderDetails;
    }
    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Boolean getRefunded() {
        return refunded;
    }

    public void setRefunded(Boolean refunded) {
        this.refunded = refunded;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public void setOrderDetails(List<OrderDetail> orderDetails) {
        this.orderDetails = orderDetails;
    }
  private BigDecimal subtotal;
 public BigDecimal getSubtotal() {return subtotal;}
 public void setSubtotal(BigDecimal value) {subtotal=value;}
  private BigDecimal shippingFee;
 public BigDecimal getShippingFee() {return shippingFee;}
 public void setShippingFee(BigDecimal value) {shippingFee=value;}
  private BigDecimal discount;
 public BigDecimal getDiscount() {return discount;}
 public void setDiscount(BigDecimal value) {discount=value;}
  private String voucherCode;
 public String getVoucherCode() {return voucherCode;}
 public void setVoucherCode(String value) {voucherCode=value;}
  private String shippingMethod;
 public String getShippingMethod() {return shippingMethod;}
 public void setShippingMethod(String value) {shippingMethod=value;}
  private String shippingCarrier;
 public String getShippingCarrier() {return shippingCarrier;}
 public void setShippingCarrier(String value) {shippingCarrier=value;}
  private String trackingCode;
 public String getTrackingCode() {return trackingCode;}
 public void setTrackingCode(String value) {trackingCode=value;}
  private LocalDateTime paymentExpiresAt;
 public LocalDateTime getPaymentExpiresAt() {return paymentExpiresAt;}
 public void setPaymentExpiresAt(LocalDateTime value) {paymentExpiresAt=value;}
 @ManyToOne private User customer;
 public User getCustomer() {return customer;}
 public void setCustomer(User value) {customer=value;}
  private String shipmentStatus = "preparing";
 public String getShipmentStatus() {return shipmentStatus;}
 public void setShipmentStatus(String value) {shipmentStatus=value;}
 @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) private java.time.LocalDate estimatedDeliveryAt;
 public java.time.LocalDate getEstimatedDeliveryAt() {return estimatedDeliveryAt;}
 public void setEstimatedDeliveryAt(java.time.LocalDate value) {estimatedDeliveryAt=value;}
 public String getOrderStatusLabel(){return switch(orderStatus==null?"":orderStatus){case "PENDING"->"Chờ xác nhận";case "CONFIRMED"->"Đã xác nhận";case "SHIPPING"->"Đang giao hàng";case "COMPLETED"->"Đã giao hàng";case "CANCELLED"->"Đã hủy";default->"Chưa xác định";};}
 public String getPaymentStatusLabel(){return switch(paymentStatus==null?"":paymentStatus){case "PENDING"->"Chờ thanh toán";case "UNPAID"->"Chưa thanh toán";case "PAID"->"Đã thanh toán";case "REFUNDED"->"Đã hoàn tiền";default->"Chưa xác định";};}
 public String getPaymentMethodLabel(){return switch(paymentMethod==null?"":paymentMethod){case "QR"->"Chuyển khoản";case "COD"->"Thanh toán khi nhận hàng";case "WALLET"->"Ví Tinh Hoa";default->"Chưa xác định";};}
 @Column(length=100) private String giftWrapName;
 private BigDecimal giftWrapFee;
 @Column(columnDefinition="TEXT") private String giftMessage;
 private Boolean hidePrices=false;
 public String getGiftWrapName(){return giftWrapName;} public void setGiftWrapName(String v){giftWrapName=v;}
 public BigDecimal getGiftWrapFee(){return giftWrapFee;} public void setGiftWrapFee(BigDecimal v){giftWrapFee=v;}
 public String getGiftMessage(){return giftMessage;} public void setGiftMessage(String v){giftMessage=v;}
 public Boolean getHidePrices(){return Boolean.TRUE.equals(hidePrices);} public void setHidePrices(Boolean v){hidePrices=v;}
}
