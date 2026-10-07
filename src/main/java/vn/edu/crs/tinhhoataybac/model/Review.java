package vn.edu.crs.tinhhoataybac.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
public class Review {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private User user;

  @ManyToOne(optional = false)
  private Product product;

  private Integer rating;

  @Column(length = 2000)
  private String comment;

  private LocalDateTime createdAt = LocalDateTime.now();

  public Long getId() {
    return id;
  }

  public void setId(Long value) {
    id = value;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User value) {
    user = value;
  }

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product value) {
    product = value;
  }

  public Integer getRating() {
    return rating;
  }

  public void setRating(Integer value) {
    rating = value;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String value) {
    comment = value;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime value) {
    createdAt = value;
  }
}
