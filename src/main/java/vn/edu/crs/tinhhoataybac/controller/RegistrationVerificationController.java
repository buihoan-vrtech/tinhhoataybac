package vn.edu.crs.tinhhoataybac.controller;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.crs.tinhhoataybac.service.RegistrationVerificationService;
@Controller
@RequestMapping("/register/verify")
public class RegistrationVerificationController {
 private final RegistrationVerificationService verification;
 public RegistrationVerificationController(RegistrationVerificationService verification){this.verification=verification;}
 @GetMapping public String page(HttpSession s,Model m){String email=verification.maskedEmail(s);if(email==null)return "redirect:/register";m.addAttribute("maskedEmail",email);return "register-verify";}
 @PostMapping public String confirm(@RequestParam String code,HttpSession s,Model m){try{String error=verification.confirm(s,code);if(error==null)return "redirect:/login?registered";m.addAttribute("error",error);}catch(org.springframework.dao.DataIntegrityViolationException|IllegalStateException e){m.addAttribute("error","Không thể hoàn tất đăng ký. Email có thể đã được sử dụng.");}return page(s,m);}
 @PostMapping("/resend") public String resend(HttpSession s,RedirectAttributes f){try{verification.resend(s);f.addFlashAttribute("success","Đã gửi mã mới. Bạn kiểm tra cả hộp thư Spam nhé.");}catch(IllegalStateException e){f.addFlashAttribute("error",e.getMessage());}return "redirect:/register/verify";}
}
