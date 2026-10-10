package vn.edu.crs.tinhhoataybac.model;
import jakarta.persistence.*;
@Entity @Table(name="bundle_components",uniqueConstraints=@UniqueConstraint(columnNames={"bundle_id","product_id"}))
public class BundleComponent{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne @JoinColumn(name="bundle_id",nullable=false) private ProductBundle bundle;
 @ManyToOne @JoinColumn(name="product_id",nullable=false) private Product product;
 private double quantity;
 public Long getId(){return id;}
 public ProductBundle getBundle(){return bundle;} public void setBundle(ProductBundle v){bundle=v;}
 public Product getProduct(){return product;} public void setProduct(Product v){product=v;}
 public double getQuantity(){return quantity;} public void setQuantity(double v){quantity=v;}
}
