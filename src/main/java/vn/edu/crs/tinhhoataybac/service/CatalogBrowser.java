package vn.edu.crs.tinhhoataybac.service;

import java.text.Normalizer;
import java.util.*;
import vn.edu.crs.tinhhoataybac.model.Product;

public final class CatalogBrowser {
  private CatalogBrowser() {}

  public static String normalize(String text) {
    return Normalizer.normalize(text == null ? "" : text.trim().toLowerCase(Locale.ROOT),
        Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace('đ', 'd');
  }

  public static Double priceBound(Double value) {
    return value != null && Double.isFinite(value) && value >= 0 ? value : null;
  }

  public static String sortKey(String value) {
    return Set.of("newest", "price_asc", "price_desc", "name").contains(value == null ? "" : value)
        ? value : "newest";
  }

  public record Result(List<Product> items, int total, int page, int pages, int first, int last) {}

  public static Result browse(List<Product> source, String keyword, Long categoryId,
      Double minPrice, Double maxPrice, boolean inStock, boolean offers, String sort, int page) {
    String query = normalize(keyword);
    Double min = priceBound(minPrice), max = priceBound(maxPrice);
    Comparator<Product> order = switch (sortKey(sort)) {
      case "price_asc" -> Comparator.comparing(Product::getEffectivePrice);
      case "price_desc" -> Comparator.comparing(Product::getEffectivePrice).reversed();
      case "name" -> Comparator.comparing(p -> normalize(p.getName()));
      default -> Comparator.comparing(Product::getId).reversed();
    };
    List<Product> matched = source.stream()
        .filter(p -> p.getPrice() != null && Double.isFinite(p.getPrice()))
        .filter(p -> query.isEmpty() || normalize(p.getName()).contains(query))
        .filter(p -> categoryId == null || p.getCategory() != null && categoryId.equals(p.getCategory().getId()))
        .filter(p -> min == null || p.getEffectivePrice() >= min)
        .filter(p -> max == null || p.getEffectivePrice() <= max)
        .filter(p -> !inStock || p.getStock() != null && p.getStock() > 0)
        .filter(p -> !offers || p.getEffectivePrice() < p.getPrice())
        .sorted(order.thenComparing(Product::getId)).toList();
    int pages = Math.max(1, (matched.size() + 8) / 9);
    int current = Math.max(0, Math.min(page, pages - 1));
    int from = current * 9, to = Math.min(from + 9, matched.size());
    return new Result(matched.subList(from, to), matched.size(), current, pages,
        matched.isEmpty() ? 0 : from + 1, to);
  }
}
