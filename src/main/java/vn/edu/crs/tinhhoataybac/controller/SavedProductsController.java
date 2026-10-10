package vn.edu.crs.tinhhoataybac.controller;

import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.crs.tinhhoataybac.repository.ProductRepository;

@Controller
public class SavedProductsController {
  private final ProductRepository products;

  public SavedProductsController(ProductRepository products) { this.products = products; }

  @GetMapping("/products/favorites")
  public String favorites(Model model) {
    model.addAttribute("collectionMode", "favorites");
    model.addAttribute("collectionTitle", "Sản phẩm yêu thích");
    return "saved-products";
  }

  @GetMapping("/products/recent")
  public String recent(Model model) {
    model.addAttribute("collectionMode", "recent");
    model.addAttribute("collectionTitle", "Sản phẩm đã xem");
    return "saved-products";
  }

  public static List<Long> parseIds(String input) {
    if (input == null || input.length() > 2200) return List.of();
    Set<Long> ids = new LinkedHashSet<>();
    for (String token : input.split(",")) {
      if (ids.size() == 100) break;
      try {
        long id = Long.parseLong(token.trim());
        if (id > 0) ids.add(id);
      } catch (NumberFormatException ignored) { }
    }
    return List.copyOf(ids);
  }

  public record Card(Long id, String name, String image, Double price, String category, boolean available) {}

  @GetMapping("/products/collection-data")
  @ResponseBody
  public List<Card> collection(@RequestParam(defaultValue = "") String ids) {
    List<Long> requested = parseIds(ids);
    if (requested.isEmpty()) return List.of();
    var indexed = new HashMap<Long, Card>();
    for (var p : products.findAllById(requested)) {
      if (p.getPrice() == null || !Double.isFinite(p.getPrice())) continue;
      indexed.put(p.getId(), new Card(p.getId(), p.getName(), p.getImage(), p.getEffectivePrice(),
          p.getCategory() == null ? "Đặc sản Tây Bắc" : p.getCategory().getName(),
          p.getStock() != null && p.getStock() > 0));
    }
    return requested.stream().map(indexed::get).filter(Objects::nonNull).toList();
  }
}
