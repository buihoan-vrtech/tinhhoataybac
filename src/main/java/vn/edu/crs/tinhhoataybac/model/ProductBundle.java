package vn.edu.crs.tinhhoataybac.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;
@Entity @Table(name="product_bundles")
public class ProductBundle {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=150) private String name;
 @Column(length=1000) private String description;
 private boolean active;
 @OneToMany(mappedBy="bundle",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("id ASC") private List<BundleComponent> components=new ArrayList<>();
 public Long getId(){return id;} public void setId(Long v){id=v;}
 public String getName(){return name;} public void setName(String v){name=v;}
 public String getDescription(){return description;} public void setDescription(String v){description=v;}
 public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
 public List<BundleComponent> getComponents(){return components;}
 public BigDecimal getPrice(){return components.stream().map(c->BigDecimal.valueOf(c.getProduct().getEffectivePrice()).multiply(BigDecimal.valueOf(c.getQuantity()))).reduce(BigDecimal.ZERO,BigDecimal::add);}
 public boolean isAvailable(){return active&&!components.isEmpty()&&components.stream().allMatch(c->c.getProduct().getStock()!=null&&c.getProduct().getStock()>=c.getQuantity());}
}
