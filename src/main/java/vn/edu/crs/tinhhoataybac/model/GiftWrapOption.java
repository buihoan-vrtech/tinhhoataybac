package vn.edu.crs.tinhhoataybac.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="gift_wrap_options")
public class GiftWrapOption{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=100) private String name;
 @Column(length=500) private String description;
 @Column(nullable=false,precision=15,scale=2) private BigDecimal price;
 private boolean active;
 public Long getId(){return id;} public void setId(Long v){id=v;}
 public String getName(){return name;} public void setName(String v){name=v;}
 public String getDescription(){return description;} public void setDescription(String v){description=v;}
 public BigDecimal getPrice(){return price;} public void setPrice(BigDecimal v){price=v;}
 public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
}
