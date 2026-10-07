package vn.edu.crs.tinhhoataybac.controller;

import java.math.*;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.crs.tinhhoataybac.model.*;
import vn.edu.crs.tinhhoataybac.repository.*;

@Controller
@RequestMapping("/admin")
public class CatalogAdminController {
  private final vn.edu.crs.tinhhoataybac.service.ImageStorageService storage;
  private final ProductRepository products;
  private final CategoryRepository categories;
  private final VoucherRepository vouchers;
  private final ProductImageRepository images;
  private final OrderRepository orders;
  private final UserRepository users;
  private final WalletRepository wallets;
  private final WalletTransactionRepository transactions;

  public CatalogAdminController(
      ProductRepository products,
      CategoryRepository categories,
      VoucherRepository vouchers,
      ProductImageRepository images,
      OrderRepository orders,
      UserRepository users,
      WalletRepository wallets,
      WalletTransactionRepository transactions,
      vn.edu.crs.tinhhoataybac.service.ImageStorageService storage) {
    this.storage = storage;
    this.products = products;
    this.categories = categories;
    this.vouchers = vouchers;
    this.images = images;
    this.orders = orders;
    this.users = users;
    this.wallets = wallets;
    this.transactions = transactions;
  }

  @GetMapping("/dashboard")
  public String dashboard(Model m) {
    m.addAttribute("productCount", products.count());
    m.addAttribute("userCount", users.count());
    m.addAttribute("orderCount", orders.count());
    m.addAttribute(
        "revenue",
        orders.findAll().stream()
            .filter(o -> "PAID".equals(o.getPaymentStatus()))
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
    m.addAttribute(
        "orders",
        orders.findAll().stream()
            .sorted(Comparator.comparing(Order::getId).reversed())
            .limit(10)
            .toList());
    return "admin/overview";
  }

  @GetMapping("/products")
  public String products(@RequestParam(required = false) Long edit, Model m) {
    m.addAttribute("products", products.findAll());
    m.addAttribute("categories", categories.findAll());
    m.addAttribute("product", edit == null ? new Product() : products.findById(edit).orElseThrow());
    m.addAttribute(
        "gallery", edit == null ? java.util.List.of() : images.findByProductIdOrderByIdAsc(edit));
    return "admin/catalog";
  }

  @PostMapping("/products/save")
  public String saveProduct(
      @ModelAttribute Product form,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) String gallery,
      @RequestParam(required = false) org.springframework.web.multipart.MultipartFile imageFile,
      @RequestParam(required = false)
          org.springframework.web.multipart.MultipartFile[] galleryFiles,
      RedirectAttributes flash) {
    try {
      if (form.getName() == null
          || form.getName().isBlank()
          || form.getPrice() == null
          || !Double.isFinite(form.getPrice())
          || form.getPrice() <= 0
          || form.getStock() == null
          || !Double.isFinite(form.getStock())
          || form.getStock() < 0
          || form.getMinQuantity() == null
          || !Double.isFinite(form.getMinQuantity())
          || form.getMinQuantity() <= 0
          || form.getQuantityStep() == null
          || !Double.isFinite(form.getQuantityStep())
          || form.getQuantityStep() <= 0)
        throw new IllegalStateException("Nhập tên, giá dương và tồn kho không âm.");
      Product p =
          form.getId() == null ? new Product() : products.findById(form.getId()).orElseThrow();
      p.setName(form.getName().trim());
      p.setPrice(form.getPrice());
      p.setStock(form.getStock());
      p.setImage(form.getImage());
      String uploaded = storage.save(imageFile);
      if (uploaded != null) p.setImage(uploaded);
      p.setDescription(form.getDescription());
      p.setFeatured(Boolean.TRUE.equals(form.getFeatured()));
      p.setUnit(form.getUnit());
      p.setMinQuantity(form.getMinQuantity());
      p.setQuantityStep(form.getQuantityStep());
      if (form.getPromotionPrice() != null
          && (form.getPromotionPrice() < 0
              || !Double.isFinite(form.getPromotionPrice())
              || form.getPromotionPrice() >= form.getPrice()))
        throw new IllegalStateException("Giá khuyến mãi phải nhỏ hơn giá gốc.");
      p.setPromotionStartsAt(form.getPromotionStartsAt());
      p.setPromotionPrice(form.getPromotionPrice());
      p.setPromotionExpiresAt(form.getPromotionExpiresAt());
      p.setCategory(categoryId == null ? null : categories.findById(categoryId).orElseThrow());
      products.save(p);
      if (gallery != null && !gallery.isBlank())
        for (String url : gallery.split("\\R")) {
          if (!url.isBlank()) {
            ProductImage i = new ProductImage();
            i.setProduct(p);
            i.setUrl(url.trim());
            images.save(i);
          }
        }
      if (galleryFiles != null) {
        if (galleryFiles.length > 10)
          throw new IllegalStateException("Mỗi lần thêm tối đa 10 ảnh.");
        for (var file : galleryFiles) {
          String url = storage.save(file);
          if (url != null) {
            ProductImage i = new ProductImage();
            i.setProduct(p);
            i.setUrl(url);
            images.save(i);
          }
        }
      }
      flash.addFlashAttribute("success", "Đã lưu sản phẩm.");
    } catch (RuntimeException e) {
      flash.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/admin/products";
  }

  @PostMapping("/products/{id}/delete")
  public String deleteProduct(@PathVariable Long id, RedirectAttributes f) {
    try {
      Product p = products.findById(id).orElseThrow();
      if (orders.findAll().stream()
          .flatMap(o -> o.getOrderDetails().stream())
          .anyMatch(d -> d.getProduct().getId().equals(id)))
        throw new IllegalStateException("Sản phẩm đã có trong đơn hàng, không thể xóa.");
      images.deleteAll(images.findByProductIdOrderByIdAsc(id));
      products.delete(p);
      f.addFlashAttribute("success", "Đã xóa sản phẩm.");
    } catch (RuntimeException e) {
      f.addFlashAttribute("error", "Không thể xóa sản phẩm đang được sử dụng.");
    }
    return "redirect:/admin/products";
  }

  @GetMapping("/categories")
  public String categories(@RequestParam(required = false) Long edit, Model m) {
    m.addAttribute("categories", categories.findAll());
    m.addAttribute(
        "category", edit == null ? new Category() : categories.findById(edit).orElseThrow());
    return "admin/categories";
  }

  @PostMapping("/categories/save")
  public String category(@ModelAttribute Category form, RedirectAttributes f) {
    try {
      if (form.getName() == null || form.getName().isBlank())
        throw new IllegalStateException("Nhập tên danh mục.");
      Category c =
          form.getId() == null ? new Category() : categories.findById(form.getId()).orElseThrow();
      c.setName(form.getName().trim());
      c.setDescription(form.getDescription());
      categories.save(c);
      f.addFlashAttribute("success", "Đã lưu danh mục.");
    } catch (RuntimeException e) {
      f.addFlashAttribute("error", "Tên danh mục không hợp lệ hoặc đã tồn tại.");
    }
    return "redirect:/admin/categories";
  }

  @PostMapping("/categories/{id}/delete")
  public String deleteCategory(@PathVariable Long id, RedirectAttributes f) {
    if (!products.findByCategoryId(id).isEmpty())
      f.addFlashAttribute("error", "Danh mục còn sản phẩm.");
    else {
      categories.deleteById(id);
      f.addFlashAttribute("success", "Đã xóa danh mục.");
    }
    return "redirect:/admin/categories";
  }

  @GetMapping("/vouchers")
  public String vouchers(@RequestParam(required = false) Long edit, Model m) {
    m.addAttribute("vouchers", vouchers.findAll());
    m.addAttribute("voucher", edit == null ? new Voucher() : vouchers.findById(edit).orElseThrow());
    return "admin/vouchers";
  }

  @PostMapping("/vouchers/save")
  public String voucher(@ModelAttribute Voucher form, RedirectAttributes f) {
    try {
      if (form.getCode() == null
          || !form.getCode().matches("[A-Za-z0-9_-]{2,40}")
          || !Set.of("fixed", "percent", "shipping").contains(form.getType())
          || form.getValue() == null
          || form.getValue().signum() < 0
          || form.getMinOrderValue() == null
          || form.getMinOrderValue().signum() < 0
          || ("percent".equals(form.getType())
              && form.getValue().compareTo(new BigDecimal("100")) > 0)
          || (form.getMaxDiscount() != null && form.getMaxDiscount().signum() < 0)
          || (form.getUsageLimit() != null && form.getUsageLimit() < 1)
          || (form.getStartsAt() != null
              && form.getExpiresAt() != null
              && !form.getStartsAt().isBefore(form.getExpiresAt())))
        throw new IllegalStateException("Thông tin voucher không hợp lệ.");
      Voucher v =
          form.getId() == null ? new Voucher() : vouchers.findById(form.getId()).orElseThrow();
      v.setCode(form.getCode().trim().toUpperCase(Locale.ROOT));
      v.setName(form.getName());
      v.setType(form.getType());
      v.setValue(form.getValue());
      v.setMinOrderValue(form.getMinOrderValue());
      v.setMaxDiscount(form.getMaxDiscount());
      v.setUsageLimit(form.getUsageLimit());
      v.setStartsAt(form.getStartsAt());
      v.setExpiresAt(form.getExpiresAt());
      v.setActive(Boolean.TRUE.equals(form.getActive()));
      vouchers.save(v);
      f.addFlashAttribute("success", "Đã lưu voucher.");
    } catch (RuntimeException e) {
      f.addFlashAttribute("error", "Voucher không hợp lệ hoặc mã đã tồn tại.");
    }
    return "redirect:/admin/vouchers";
  }

  @PostMapping("/vouchers/{id}/disable")
  public String disable(@PathVariable Long id) {
    Voucher v = vouchers.findById(id).orElseThrow();
    v.setActive(false);
    vouchers.save(v);
    return "redirect:/admin/vouchers";
  }

  @GetMapping("/wallets")
  public String wallets(Model m) {
    m.addAttribute("wallets", wallets.findAll());
    m.addAttribute(
        "transactions",
        transactions.findAll().stream()
            .sorted(Comparator.comparing(WalletTransaction::getId).reversed())
            .limit(100)
            .toList());
    return "admin/wallets";
  }

  @PostMapping("/products/{id}/gallery/{imageId}/delete")
  public String deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
    var image = images.findById(imageId).orElseThrow();
    if (!image.getProduct().getId().equals(id))
      throw new IllegalStateException("Ảnh không thuộc sản phẩm.");
    images.delete(image);
    return "redirect:/admin/products?edit=" + id;
  }
}
