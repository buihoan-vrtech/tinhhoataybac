package vn.edu.crs.tinhhoataybac.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "sepay_transactions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "sepayTransactionId")
        }
)
public class SePayTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long sepayTransactionId;

    private String gateway;

    private String transactionDate;

    private String accountNumber;

    private String paymentCode;

    @Column(length = 1000)
    private String content;

    private String transferType;

    @Column(
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal transferAmount;

    private String referenceCode;

    private LocalDateTime receivedAt;

    public SePayTransaction() {
    }

    @PrePersist
    public void prePersist() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getSepayTransactionId() {
        return sepayTransactionId;
    }

    public void setSepayTransactionId(Long sepayTransactionId) {
        this.sepayTransactionId = sepayTransactionId;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getPaymentCode() {
        return paymentCode;
    }

    public void setPaymentCode(String paymentCode) {
        this.paymentCode = paymentCode;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTransferType() {
        return transferType;
    }

    public void setTransferType(String transferType) {
        this.transferType = transferType;
    }

    public BigDecimal getTransferAmount() {
        return transferAmount;
    }

    public void setTransferAmount(BigDecimal transferAmount) {
        this.transferAmount = transferAmount;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }
}