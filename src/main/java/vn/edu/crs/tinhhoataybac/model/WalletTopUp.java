package vn.edu.crs.tinhhoataybac.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(
        name = "wallet_topups",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = "payment_code"
                )
        }
)
public class WalletTopUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;


    @ManyToOne
    @JoinColumn(
            name = "wallet_id",
            nullable = false
    )
    private Wallet wallet;


    @Column(
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal amount;


    @Column(
            name = "payment_code",
            unique = true
    )
    private String paymentCode;


    @Column(nullable = false)
    private String status;


    private LocalDateTime createdAt;


    private LocalDateTime paidAt;


    public WalletTopUp() {
    }


    @PrePersist
    public void prePersist() {

        if (status == null) {
            status = "PENDING";
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }


    public Long getId() {
        return id;
    }


    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }


    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }


    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }


    public String getPaymentCode() {
        return paymentCode;
    }

    public void setPaymentCode(String paymentCode) {
        this.paymentCode = paymentCode;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}