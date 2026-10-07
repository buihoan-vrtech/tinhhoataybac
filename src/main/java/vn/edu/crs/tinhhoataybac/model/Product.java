package vn.edu.crs.tinhhoataybac.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private Double price;

    private String image;

    @Column(length = 2000)
    private String description;

    private Double stock;

    private Boolean featured = false;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    public Product() {
    }

    public Product(Long id, String name, Double price, String image,
                   String description, Double stock,
                   Boolean featured, Category category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.image = image;
        this.description = description;
        this.stock = stock;
        this.featured = featured;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getStock() {
        return stock;
    }

    public void setStock(Double stock) {
        this.stock = stock;
    }

    public Boolean getFeatured() {
        return featured;
    }

    public void setFeatured(Boolean featured) {
        this.featured = featured;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
  private Double promotionPrice;
 public Double getPromotionPrice() {return promotionPrice;}
 public void setPromotionPrice(Double value) {promotionPrice=value;}
  @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) private LocalDateTime promotionExpiresAt;
 public LocalDateTime getPromotionExpiresAt() {return promotionExpiresAt;}
 public void setPromotionExpiresAt(LocalDateTime value) {promotionExpiresAt=value;}
  private String unit = "sản phẩm";
 public String getUnit() {return unit;}
 public void setUnit(String value) {unit=value;}
 public Double getEffectivePrice() {
 return promotionPrice!=null && promotionPrice>=0 && promotionPrice<price && (promotionStartsAt==null || !promotionStartsAt.isAfter(LocalDateTime.now())) && (promotionExpiresAt==null || promotionExpiresAt.isAfter(LocalDateTime.now())) ? promotionPrice : price;
 }
  private Double minQuantity = 1.0;
 public Double getMinQuantity() {return minQuantity;}
 public void setMinQuantity(Double value) {minQuantity=value;}
  private Double quantityStep = 1.0;
 public Double getQuantityStep() {return quantityStep;}
 public void setQuantityStep(Double value) {quantityStep=value;}
  @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) private LocalDateTime promotionStartsAt;
 public LocalDateTime getPromotionStartsAt() {return promotionStartsAt;}
 public void setPromotionStartsAt(LocalDateTime value) {promotionStartsAt=value;}
 public double getMinimumOrderQuantity(){return Math.max(minQuantity==null?1:minQuantity,"kg".equalsIgnoreCase(unit)?0.5:"g".equalsIgnoreCase(unit)?100:0.01);}
 public double getOrderQuantityStep(){return Math.max(quantityStep==null?1:quantityStep,"kg".equalsIgnoreCase(unit)?0.25:"g".equalsIgnoreCase(unit)?50:0.01);}
 public void validateQuantity(double quantity){
  if(!Double.isFinite(quantity)||quantity<getMinimumOrderQuantity()||java.math.BigDecimal.valueOf(quantity).stripTrailingZeros().scale()>2||java.math.BigDecimal.valueOf(quantity).subtract(java.math.BigDecimal.valueOf(getMinimumOrderQuantity())).remainder(java.math.BigDecimal.valueOf(getOrderQuantityStep())).signum()!=0)throw new IllegalStateException("Số lượng tối thiểu "+getMinimumOrderQuantity()+" "+unit+", bước tăng "+getOrderQuantityStep()+".");
 }
}