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
        return getBatchTracked()?Math.round(getSaleBatches().stream().mapToDouble(ProductBatch::getRemainingQuantity).sum()*100.0)/100.0:stock;
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

 @Column(length=255) private String origin;
 public String getOrigin(){return origin;} public void setOrigin(String value){origin=value;}

 @Column(length=255) private String producer;
 public String getProducer(){return producer;} public void setProducer(String value){producer=value;}

 @Column(columnDefinition="TEXT") private String ingredients;
 public String getIngredients(){return ingredients;} public void setIngredients(String value){ingredients=value;}

 @Column(columnDefinition="TEXT") private String allergenInfo;
 public String getAllergenInfo(){return allergenInfo;} public void setAllergenInfo(String value){allergenInfo=value;}

 @Column(columnDefinition="TEXT") private String storageInstructions;
 public String getStorageInstructions(){return storageInstructions;} public void setStorageInstructions(String value){storageInstructions=value;}

 @Column(columnDefinition="TEXT") private String usageInstructions;
 public String getUsageInstructions(){return usageInstructions;} public void setUsageInstructions(String value){usageInstructions=value;}

 @Column(length=255) private String shelfLife;
 public String getShelfLife(){return shelfLife;} public void setShelfLife(String value){shelfLife=value;}

 @Column(length=80) private String familyCode;
 public String getFamilyCode(){return familyCode;} public void setFamilyCode(String value){familyCode=value;}

 @Column(length=100) private String variantLabel;
 public String getVariantLabel(){return variantLabel;} public void setVariantLabel(String value){variantLabel=value;}

 private Boolean batchTracked=false;
 public Boolean getBatchTracked(){return Boolean.TRUE.equals(batchTracked);}
 public void setBatchTracked(Boolean v){batchTracked=v;}
 @OneToMany(mappedBy="product",fetch=FetchType.EAGER) private java.util.List<ProductBatch> batches=new java.util.ArrayList<>();
 public java.util.List<ProductBatch> getBatches(){return batches;}
 public java.util.List<ProductBatch> getSaleBatches(){return batches.stream().filter(b->b.isSellable(java.time.LocalDate.now())).sorted(java.util.Comparator.comparing(ProductBatch::getExpiresOn).thenComparing(ProductBatch::getId,java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))).toList();}
 public java.time.LocalDate getNextExpiry(){return getSaleBatches().stream().map(ProductBatch::getExpiresOn).findFirst().orElse(null);}
}
