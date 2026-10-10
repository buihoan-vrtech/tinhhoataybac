package vn.edu.crs.tinhhoataybac.model;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity @Table(name="product_batches",uniqueConstraints=@UniqueConstraint(columnNames={"product_id","batch_code"}))
public class ProductBatch {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne @JoinColumn(name="product_id",nullable=false) private Product product;
 @Column(name="batch_code",nullable=false,length=80) private String batchCode;
 @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) @Column(nullable=false) private LocalDate manufacturedOn;
 @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) @Column(nullable=false) private LocalDate expiresOn;
 @Column(nullable=false) private Double remainingQuantity;
 public Long getId(){return id;} public void setId(Long v){id=v;}
 public Product getProduct(){return product;} public void setProduct(Product v){product=v;}
 public String getBatchCode(){return batchCode;} public void setBatchCode(String v){batchCode=v;}
 public LocalDate getManufacturedOn(){return manufacturedOn;} public void setManufacturedOn(LocalDate v){manufacturedOn=v;}
 public LocalDate getExpiresOn(){return expiresOn;} public void setExpiresOn(LocalDate v){expiresOn=v;}
 public Double getRemainingQuantity(){return remainingQuantity;} public void setRemainingQuantity(Double v){remainingQuantity=v;}
 public boolean isSellable(LocalDate today){return remainingQuantity!=null&&remainingQuantity>0&&manufacturedOn!=null&&!manufacturedOn.isAfter(today)&&expiresOn!=null&&expiresOn.isAfter(today);}
 public String getStatus(){return expiresOn==null?"Thiếu hạn dùng":!expiresOn.isAfter(LocalDate.now())?"Hết hạn":manufacturedOn.isAfter(LocalDate.now())?"Chưa đến ngày sản xuất":expiresOn.isBefore(LocalDate.now().plusDays(31))?"Sắp hết hạn (30 ngày)":"Còn hạn";}
}
