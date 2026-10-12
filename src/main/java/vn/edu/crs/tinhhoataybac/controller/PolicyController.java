package vn.edu.crs.tinhhoataybac.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PolicyController {
  private final vn.edu.crs.tinhhoataybac.service.BankQrService bankQr;
  public PolicyController(vn.edu.crs.tinhhoataybac.service.BankQrService bankQr) { this.bankQr=bankQr; }
  @GetMapping("/chinh-sach-giao-hang")
  public String shipping() {
    return "policies/shipping";
  }

  @GetMapping("/chinh-sach-doi-tra")
  public String returns() {
    return "policies/returns";
  }

  @GetMapping("/chinh-sach-bao-mat")
  public String privacy() {
    return "policies/privacy";
  }

  @GetMapping("/dieu-khoan-dich-vu")
  public String terms() {
    return "policies/terms";
  }

  @GetMapping("/chinh-sach-thanh-toan")
  public String payment(org.springframework.ui.Model model) {
    model.addAttribute("paymentBank",bankQr.bank());
    model.addAttribute("paymentAccount",bankQr.account());
    model.addAttribute("paymentHolder",bankQr.holder());
    return "policies/payment";
  }
}
