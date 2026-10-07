package vn.edu.crs.tinhhoataybac.model;

import jakarta.persistence.*;

@Entity
public class ProductImage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private Product product;

  @Column(length = 1000)
  private String url;

  public Long getId() {
    return id;
  }

  public void setId(Long value) {
    id = value;
  }

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product value) {
    product = value;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String value) {
    url = value;
  }
}
