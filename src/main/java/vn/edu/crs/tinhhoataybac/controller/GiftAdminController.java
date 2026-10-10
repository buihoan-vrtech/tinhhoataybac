package vn.edu.crs.tinhhoataybac.controller;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.crs.tinhhoataybac.model.*;
import vn.edu.crs.tinhhoataybac.repository.*;
@Controller @RequestMapping("/admin/gifts") public class GiftAdminController{
 private final ProductBundleRepository bundles;private final ProductRepository products;private final GiftWrapOptionRepository wraps;
 public GiftAdminController(ProductBundleRepository b,ProductRepository p,GiftWrapOptionRepository w){bundles=b;products=p;wraps=w;}
 @GetMapping public String page(@RequestParam(required=false)Long edit,Model m){m.addAttribute("bundles",bundles.findAll());m.addAttribute("products",products.findAll());m.addAttribute("wraps",wraps.findAll());m.addAttribute("bundle",edit==null?new ProductBundle():bundles.findById(edit).orElseThrow());return "admin/gifts";}
 @PostMapping("/bundles/save") @Transactional public String saveBundle(@RequestParam(required=false)Long id,@RequestParam String name,@RequestParam(defaultValue="")String description,@RequestParam(defaultValue="false")boolean active,@RequestParam List<String> productIds,@RequestParam List<String> quantities){
  if(name.isBlank()||name.length()>150||description.length()>1000||productIds.size()!=quantities.size()||productIds.size()>6)throw new IllegalStateException("Tên combo và danh sách món không hợp lệ.");
  List<BundleComponent> entries=new ArrayList<>();Set<Long> used=new HashSet<>();
  for(int i=0;i<productIds.size();i++){if(productIds.get(i).isBlank())continue;long pid=Long.parseLong(productIds.get(i));if(!used.add(pid))throw new IllegalStateException("Mỗi sản phẩm chỉ xuất hiện một lần trong combo.");var p=products.findById(pid).orElseThrow();double q=Double.parseDouble(quantities.get(i));p.validateQuantity(q);var c=new BundleComponent();c.setProduct(p);c.setQuantity(q);entries.add(c);}
  if(entries.size()<2)throw new IllegalStateException("Combo cần ít nhất 2 sản phẩm.");
  var b=id==null?new ProductBundle():bundles.findById(id).orElseThrow();b.setName(name.trim());b.setDescription(description.trim());b.setActive(active);b.getComponents().clear();bundles.saveAndFlush(b);for(var c:entries){c.setBundle(b);b.getComponents().add(c);}bundles.save(b);return "redirect:/admin/gifts";
 }
 @PostMapping("/wraps/save") @Transactional public String saveWrap(@ModelAttribute GiftWrapOption form){
  if(form.getName()==null||form.getName().isBlank()||form.getName().length()>100||form.getDescription()!=null&&form.getDescription().length()>500||form.getPrice()==null||form.getPrice().signum()<0||form.getPrice().stripTrailingZeros().scale()>0||form.getPrice().compareTo(new java.math.BigDecimal("1000000000"))>0)throw new IllegalStateException("Tên và phí hộp quà không hợp lệ (VND nguyên, không âm).");
  var w=form.getId()==null?new GiftWrapOption():wraps.findById(form.getId()).orElseThrow();w.setName(form.getName().trim());w.setDescription(form.getDescription());w.setPrice(form.getPrice());w.setActive(form.isActive());wraps.save(w);return "redirect:/admin/gifts";
 }
 @ExceptionHandler({IllegalStateException.class,NumberFormatException.class}) public String invalid(Exception e,RedirectAttributes f){f.addFlashAttribute("error",e.getMessage());return "redirect:/admin/gifts";}
}
