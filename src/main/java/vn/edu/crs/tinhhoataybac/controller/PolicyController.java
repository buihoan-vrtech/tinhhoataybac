package vn.edu.crs.tinhhoataybac.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PolicyController {
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
  public String payment() {
    return "policies/payment";
  }
}
