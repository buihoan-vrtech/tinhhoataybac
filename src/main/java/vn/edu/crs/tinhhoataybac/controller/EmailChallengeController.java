package vn.edu.crs.tinhhoataybac.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.crs.tinhhoataybac.service.EmailChallengeService;

@Controller
public class EmailChallengeController {
  private final EmailChallengeService codes;

  public EmailChallengeController(EmailChallengeService codes) {
    this.codes = codes;
  }

  @GetMapping("/forgot-password")
  public String forgot() {
    return "forgot-password";
  }

  @PostMapping("/forgot-password")
  public String send(@RequestParam String email, RedirectAttributes f) {
    try {
      codes.send(email, "reset");
      f.addFlashAttribute("success", "Nếu tài khoản tồn tại, mã xác nhận đã được gửi tới email.");
    } catch (RuntimeException e) {
      f.addFlashAttribute(
          "error", "Chưa gửi được email. Kiểm tra cấu hình SMTP hoặc thử lại sau một phút.");
    }
    return "redirect:/forgot-password";
  }

  @PostMapping("/reset-password")
  public String reset(
      @RequestParam String email,
      @RequestParam String code,
      @RequestParam String password,
      @RequestParam String confirmation,
      RedirectAttributes f) {
    if (!password.equals(confirmation)) {
      f.addFlashAttribute("error", "Mật khẩu xác nhận không khớp.");
      return "redirect:/forgot-password";
    }
    String error = codes.confirm(email, "reset", code, password);
    if (error != null) {
      f.addFlashAttribute("error", error);
      return "redirect:/forgot-password";
    }
    f.addFlashAttribute("success", "Đã đặt lại mật khẩu. Bạn có thể đăng nhập.");
    return "redirect:/login";
  }

  @PostMapping("/account/verify-email/send")
  public String sendVerify(Authentication a, RedirectAttributes f) {
    try {
      codes.send(a.getName(), "verify");
      f.addFlashAttribute("success", "Đã gửi mã xác thực email.");
    } catch (RuntimeException e) {
      f.addFlashAttribute(
          "error", "Chưa gửi được email. Kiểm tra cấu hình SMTP hoặc thử lại sau một phút.");
    }
    return "redirect:/account/profile";
  }

  @PostMapping("/account/verify-email")
  public String verify(Authentication a, @RequestParam String code, RedirectAttributes f) {
    String error = codes.confirm(a.getName(), "verify", code, null);
    f.addFlashAttribute(
        error == null ? "success" : "error", error == null ? "Email đã được xác thực." : error);
    return "redirect:/account/profile";
  }
}
