package vn.edu.crs.tinhhoataybac.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.crs.tinhhoataybac.model.*;
import vn.edu.crs.tinhhoataybac.repository.*;
import vn.edu.crs.tinhhoataybac.service.*;

@Controller
public class CustomerFeaturesController {
  private final ImageStorageService storage;
  private final UserService users;
  private final UserRepository userRepo;
  private final UserAddressRepository addresses;
  private final ReviewRepository reviews;
  private final ProductRepository products;
  private final OrderRepository orders;
  private final CustomerNotificationRepository notifications;
  private final CommerceService commerce;
  private final PasswordEncoder encoder;

  public CustomerFeaturesController(
      UserService users,
      UserRepository userRepo,
      UserAddressRepository addresses,
      ReviewRepository reviews,
      ProductRepository products,
      OrderRepository orders,
      CustomerNotificationRepository notifications,
      CommerceService commerce,
      PasswordEncoder encoder,
      ImageStorageService storage) {
    this.storage = storage;
    this.users = users;
    this.userRepo = userRepo;
    this.addresses = addresses;
    this.reviews = reviews;
    this.products = products;
    this.orders = orders;
    this.notifications = notifications;
    this.commerce = commerce;
    this.encoder = encoder;
  }

  private User user(Authentication a) {
    return users.findByEmail(a.getName());
  }

  @GetMapping("/account/profile")
  public String profile(Authentication a, Model m) {
    m.addAttribute("user", user(a));
    return "profile";
  }

  @PostMapping("/account/profile")
  public String saveProfile(
      Authentication a,
      @RequestParam String fullName,
      @RequestParam(required = false) String phone,
      @RequestParam(required = false) String address,
      @RequestParam(required = false) String avatar,
      @RequestParam(required = false) org.springframework.web.multipart.MultipartFile avatarFile,
      RedirectAttributes f) {
    try {
      if (fullName.isBlank()
          || fullName.length() > 255
          || (phone != null && !phone.isBlank() && !phone.matches("[+0-9 ()-]{8,20}"))
          || (address != null && address.length() > 500)
          || (avatar != null && avatar.length() > 1000))
        throw new IllegalStateException("Thông tin hồ sơ không hợp lệ.");
      User u = user(a);
      u.setFullName(fullName.trim());
      u.setPhone(phone);
      u.setAddress(address);
      u.setAvatar(avatar);
      String uploaded = storage.save(avatarFile);
      if (uploaded != null) u.setAvatar(uploaded);
      userRepo.save(u);
      f.addFlashAttribute("success", "Đã cập nhật hồ sơ.");
    } catch (IllegalStateException e) {
      f.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/account/profile";
  }

  @PostMapping("/account/password")
  public String password(
      Authentication a,
      @RequestParam String currentPassword,
      @RequestParam String password,
      @RequestParam String passwordConfirmation,
      RedirectAttributes f) {
    User u = user(a);
    if (!encoder.matches(currentPassword, u.getPassword()))
      f.addFlashAttribute("error", "Mật khẩu hiện tại không đúng.");
    else if (password.length() < 8
        || password.length() > 72
        || !password.equals(passwordConfirmation))
      f.addFlashAttribute("error", "Mật khẩu mới cần 8–72 ký tự và xác nhận trùng khớp.");
    else {
      u.setPassword(encoder.encode(password));
      userRepo.save(u);
      f.addFlashAttribute("success", "Đã đổi mật khẩu.");
    }
    return "redirect:/account/profile";
  }

  @GetMapping("/account/addresses")
  public String addresses(Authentication a, @RequestParam(required = false) Long edit, Model m) {
    User u = user(a);
    m.addAttribute("addresses", addresses.findByUserIdOrderByDefaultAddressDescIdDesc(u.getId()));
    m.addAttribute(
        "address",
        edit == null
            ? new UserAddress()
            : addresses.findByIdAndUserId(edit, u.getId()).orElseThrow());
    return "addresses";
  }

  @PostMapping("/account/addresses/save")
  @Transactional
  public String address(Authentication a, @ModelAttribute UserAddress form, RedirectAttributes f) {
    try {
      if (form.getReceiverName() == null
          || form.getReceiverName().isBlank()
          || form.getReceiverName().length() > 255
          || form.getPhone() == null
          || !form.getPhone().matches("[+0-9 ()-]{8,20}")
          || form.getAddressDetail() == null
          || form.getAddressDetail().isBlank()
          || form.getAddressDetail().length() > 500)
        throw new IllegalStateException("Thông tin địa chỉ không hợp lệ.");
      User u = user(a);
      UserAddress d =
          form.getId() == null
              ? new UserAddress()
              : addresses.findByIdAndUserId(form.getId(), u.getId()).orElseThrow();
      d.setUser(u);
      d.setLabel(form.getLabel());
      d.setReceiverName(form.getReceiverName().trim());
      d.setPhone(form.getPhone());
      d.setProvince(form.getProvince());
      d.setDistrict(form.getDistrict());
      d.setWard(form.getWard());
      d.setAddressDetail(form.getAddressDetail());
      var all = addresses.findByUserIdOrderByDefaultAddressDescIdDesc(u.getId());
      boolean isDefault = Boolean.TRUE.equals(form.getDefaultAddress()) || all.isEmpty();
      if (isDefault)
        for (var old : all) {
          old.setDefaultAddress(false);
          addresses.save(old);
        }
      d.setDefaultAddress(isDefault);
      addresses.save(d);
      f.addFlashAttribute("success", "Đã lưu địa chỉ.");
    } catch (RuntimeException e) {
      f.addFlashAttribute("error", "Không thể lưu địa chỉ. Hãy kiểm tra thông tin.");
    }
    return "redirect:/account/addresses";
  }

  @PostMapping("/account/addresses/{id}/default")
  @Transactional
  public String defaultAddress(Authentication a, @PathVariable Long id) {
    User u = user(a);
    var target = addresses.findByIdAndUserId(id, u.getId()).orElseThrow();
    for (var old : addresses.findByUserIdOrderByDefaultAddressDescIdDesc(u.getId())) {
      old.setDefaultAddress(old.getId().equals(target.getId()));
      addresses.save(old);
    }
    return "redirect:/account/addresses";
  }

  @PostMapping("/account/addresses/{id}/delete")
  @Transactional
  public String deleteAddress(Authentication a, @PathVariable Long id) {
    User u = user(a);
    UserAddress d = addresses.findByIdAndUserId(id, u.getId()).orElseThrow();
    addresses.delete(d);
    if (Boolean.TRUE.equals(d.getDefaultAddress())) {
      var rest = addresses.findByUserIdOrderByDefaultAddressDescIdDesc(u.getId());
      if (!rest.isEmpty()) {
        rest.getFirst().setDefaultAddress(true);
        addresses.save(rest.getFirst());
      }
    }
    return "redirect:/account/addresses";
  }

  @PostMapping("/products/{id}/reviews")
  public String review(
      Authentication a,
      @PathVariable Long id,
      @RequestParam Integer rating,
      @RequestParam(required = false) String comment,
      RedirectAttributes f) {
    User u = user(a);
    try {
      if (rating < 1 || rating > 5 || (comment != null && comment.length() > 2000))
        throw new IllegalStateException("Đánh giá cần từ 1 đến 5 sao; nội dung tối đa 2000 ký tự.");
      boolean bought =
          orders.findAll().stream()
              .filter(o -> commerce.owns(o, u) && "COMPLETED".equals(o.getOrderStatus()))
              .flatMap(o -> o.getOrderDetails().stream())
              .anyMatch(d -> d.getProduct().getId().equals(id));
      if (!bought)
        throw new IllegalStateException("Bạn cần mua và nhận sản phẩm trước khi đánh giá.");
      Review r = reviews.findByUserIdAndProductId(u.getId(), id).orElseGet(Review::new);
      r.setUser(u);
      r.setProduct(products.findById(id).orElseThrow());
      r.setRating(rating);
      r.setComment(comment);
      reviews.save(r);
      f.addFlashAttribute("success", "Đã lưu đánh giá.");
    } catch (RuntimeException e) {
      f.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/products/" + id;
  }

  @GetMapping("/account/notifications")
  public String notifications(Authentication a, Model m) {
    m.addAttribute(
        "notifications", notifications.findByUserIdOrderByCreatedAtDesc(user(a).getId()));
    return "notifications";
  }

  @PostMapping("/account/notifications/read-all")
  public String readAll(Authentication a) {
    var all = notifications.findByUserIdOrderByCreatedAtDesc(user(a).getId());
    all.forEach(n -> n.setReadFlag(true));
    notifications.saveAll(all);
    return "redirect:/account/notifications";
  }

  @PostMapping("/account/notifications/{id}/read")
  public String read(Authentication a, @PathVariable Long id) {
    var n = notifications.findByIdAndUserId(id, user(a).getId()).orElseThrow();
    n.setReadFlag(true);
    notifications.save(n);
    return n.getOrderId() == null
        ? "redirect:/account/notifications"
        : "redirect:/account/orders/" + n.getOrderId();
  }

  @PostMapping("/account/orders/{id}/service-requests")
  public String request(
      Authentication a,
      @PathVariable Long id,
      @RequestParam String type,
      @RequestParam String reason,
      RedirectAttributes f) {
    try {
      commerce.request(id, user(a), type, reason);
      f.addFlashAttribute("success", "Đã gửi yêu cầu cho shop.");
    } catch (RuntimeException e) {
      f.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/account/orders/" + id;
  }
}
