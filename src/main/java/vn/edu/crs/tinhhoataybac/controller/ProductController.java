package vn.edu.crs.tinhhoataybac.controller;

import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.crs.tinhhoataybac.model.Product;
import vn.edu.crs.tinhhoataybac.repository.*;
import vn.edu.crs.tinhhoataybac.service.CatalogBrowser;

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
    m.addAttribute("variants",p.getFamilyCode()==null||p.getFamilyCode().isBlank()?List.of(p):products.findByFamilyCodeOrderByIdAsc(p.getFamilyCode()));
    m.addAttribute("reviews", list);
    m.addAttribute("ratingAverage", list.stream().mapToInt(r -> r.getRating()).average().orElse(0));
    m.addAttribute("gallery", images.findByProductIdOrderByIdAsc(id));
    m.addAttribute("relatedProducts", p.getCategory() == null ? List.of() :
        products.findByCategoryId(p.getCategory().getId()).stream()
            .filter(other -> !id.equals(other.getId()))
            .filter(other -> other.getPrice() != null && other.getStock() != null && other.getStock() > 0)
            .limit(4).toList());
    return "product-detail";
  }

  @GetMapping({"/products", "/khuyen-mai"})
  public String list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(defaultValue = "newest") String sort,
      @RequestParam(required = false) Double minPrice,
      @RequestParam(required = false) Double maxPrice,
      @RequestParam(defaultValue = "false") boolean inStock,
      @RequestParam(defaultValue = "0") int page,
      jakarta.servlet.http.HttpServletRequest request,
      Model m) {
    boolean offers = request.getRequestURI().equals("/khuyen-mai");
    var result = CatalogBrowser.browse(products.findAll(), keyword, categoryId,
        minPrice, maxPrice, inStock, offers, sort, page);
    m.addAttribute("products", result.items());
    m.addAttribute("totalProducts", result.total());
    m.addAttribute("currentPage", result.page());
    m.addAttribute("totalPages", result.pages());
    m.addAttribute("firstProduct", result.first());
    m.addAttribute("lastProduct", result.last());
    m.addAttribute("minPrice", CatalogBrowser.priceBound(minPrice));
    m.addAttribute("maxPrice", CatalogBrowser.priceBound(maxPrice));
    m.addAttribute("inStock", inStock);
    m.addAttribute("catalogPath", offers ? "/khuyen-mai" : "/products");
    m.addAttribute("catalogTitle", offers ? "Sản phẩm khuyến mãi" : categoryId == null
        ? "Tất cả sản phẩm" : categories.findById(categoryId).map(c -> c.getName()).orElse("Sản phẩm"));
    m.addAttribute("filterError", minPrice != null && maxPrice != null && minPrice > maxPrice
        ? "Giá từ phải nhỏ hơn hoặc bằng giá đến." : null);
    m.addAttribute("categories", categories.findAll());
    m.addAttribute("keyword", keyword);
    m.addAttribute("categoryId", categoryId);
    m.addAttribute("sort", CatalogBrowser.sortKey(sort));
    return "products";
  }

  @GetMapping("/products-search-suggestions")
  @ResponseBody
  public List<Map<String, Object>> suggestions(@RequestParam(defaultValue = "") String keyword) {
    if (keyword.trim().length() < 2) return List.of();
    return products.findAll().stream()
        .filter(p -> CatalogBrowser.normalize(p.getName()).contains(CatalogBrowser.normalize(keyword)))
        .filter(p -> p.getPrice() != null)
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
