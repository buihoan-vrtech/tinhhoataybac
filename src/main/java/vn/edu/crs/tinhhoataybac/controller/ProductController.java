package vn.edu.crs.tinhhoataybac.controller;

import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.crs.tinhhoataybac.model.Product;
import vn.edu.crs.tinhhoataybac.repository.*;

@Controller
public class ProductController {
  private final ProductRepository products;
  private final CategoryRepository categories;
  private final ReviewRepository reviews;
  private final ProductImageRepository images;

  public ProductController(
      ProductRepository products,
      CategoryRepository categories,
      ReviewRepository reviews,
      ProductImageRepository images) {
    this.products = products;
    this.categories = categories;
    this.reviews = reviews;
    this.images = images;
  }

  @GetMapping("/products/{id}")
  public String detail(@PathVariable Long id, Model m) {
    Product p = products.findById(id).orElse(null);
    if (p == null) return "redirect:/products";
    var list = reviews.findByProductIdOrderByCreatedAtDesc(id);
    m.addAttribute("product", p);
    m.addAttribute("reviews", list);
    m.addAttribute("ratingAverage", list.stream().mapToInt(r -> r.getRating()).average().orElse(0));
    m.addAttribute("gallery", images.findByProductIdOrderByIdAsc(id));
    return "product-detail";
  }

  @GetMapping({"/products", "/khuyen-mai"})
  public String list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(defaultValue = "newest") String sort,
      jakarta.servlet.http.HttpServletRequest request,
      Model m) {
    String q = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
    var stream =
        products.findAll().stream()
            .filter(p -> q.isEmpty() || p.getName().toLowerCase(Locale.ROOT).contains(q))
            .filter(
                p ->
                    categoryId == null
                        || (p.getCategory() != null && categoryId.equals(p.getCategory().getId())));
    if (request.getRequestURI().equals("/khuyen-mai"))
      stream = stream.filter(p -> !p.getEffectivePrice().equals(p.getPrice()));
    Comparator<Product> c =
        switch (sort) {
          case "price_asc" -> Comparator.comparing(Product::getEffectivePrice);
          case "price_desc" -> Comparator.comparing(Product::getEffectivePrice).reversed();
          case "name" -> Comparator.comparing(Product::getName);
          default -> Comparator.comparing(Product::getId).reversed();
        };
    m.addAttribute("products", stream.sorted(c).toList());
    m.addAttribute("categories", categories.findAll());
    m.addAttribute("keyword", keyword);
    m.addAttribute("categoryId", categoryId);
    m.addAttribute("sort", sort);
    return "products";
  }

  @GetMapping("/products-search-suggestions")
  @ResponseBody
  public List<Map<String, Object>> suggestions(@RequestParam(defaultValue = "") String keyword) {
    if (keyword.trim().length() < 2) return List.of();
    return products.findByNameContainingIgnoreCase(keyword.trim()).stream()
        .limit(8)
        .map(
            p ->
                Map.<String, Object>of(
                    "id",
                    p.getId(),
                    "name",
                    p.getName(),
                    "url",
                    "/products/" + p.getId(),
                    "price",
                    p.getEffectivePrice()))
        .toList();
  }
}
