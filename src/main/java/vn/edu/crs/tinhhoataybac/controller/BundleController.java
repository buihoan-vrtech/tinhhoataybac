package vn.edu.crs.tinhhoataybac.controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.crs.tinhhoataybac.repository.ProductBundleRepository;
import vn.edu.crs.tinhhoataybac.service.BundleCartService;
@Controller public class BundleController{
 private final ProductBundleRepository bundles;private final BundleCartService cart;
 public BundleController(ProductBundleRepository b,BundleCartService c){bundles=b;cart=c;}
 @GetMapping("/combos") public String list(Model m){m.addAttribute("bundles",bundles.findByActiveTrueOrderByIdAsc());return "bundles";}
 @PostMapping("/combos/{id}/add") public String add(@PathVariable Long id,jakarta.servlet.http.HttpSession s,RedirectAttributes f){try{cart.add(id,s);f.addFlashAttribute("success","Đã thêm các món trong combo vào giỏ. Bạn có thể chọn hộp quà khi thanh toán.");return "redirect:/cart";}catch(IllegalStateException e){f.addFlashAttribute("error",e.getMessage());return "redirect:/combos";}}
}
