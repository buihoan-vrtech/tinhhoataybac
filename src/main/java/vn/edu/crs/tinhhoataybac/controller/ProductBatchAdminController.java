package vn.edu.crs.tinhhoataybac.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.crs.tinhhoataybac.model.*;
import vn.edu.crs.tinhhoataybac.repository.*;
@Controller @RequestMapping("/admin/products")
public class ProductBatchAdminController{
 private final ProductRepository products;private final ProductBatchRepository batches;private final OrderRepository orders;
 public ProductBatchAdminController(ProductRepository p,ProductBatchRepository b,OrderRepository o){products=p;batches=b;orders=o;}
 @PostMapping("/{id}/batches") @Transactional
 public String add(@PathVariable Long id,@ModelAttribute ProductBatch form){
  Product p=products.lockById(id).orElseThrow();
  if(!p.getBatchTracked()&&orders.findAll().stream().filter(o->java.util.Set.of("PENDING","CONFIRMED").contains(o.getOrderStatus())).flatMap(o->o.getOrderDetails().stream()).anyMatch(d->d.getProduct().getId().equals(id)&&d.getBatch()==null))throw new IllegalStateException("Hãy xử lý các đơn đang chờ của sản phẩm trước khi chuyển sang quản lý theo lô.");
  validateQuantity(form.getRemainingQuantity());
  if(form.getBatchCode()==null||!form.getBatchCode().trim().matches("[A-Za-z0-9._/-]{1,80}")||form.getManufacturedOn()==null||form.getExpiresOn()==null||!form.getExpiresOn().isAfter(form.getManufacturedOn())||form.getManufacturedOn().isAfter(java.time.LocalDate.now()))throw new IllegalStateException("Nhập mã lô, ngày sản xuất và hạn dùng hợp lệ.");
  if(p.getBatches().stream().anyMatch(b->b.getBatchCode().equalsIgnoreCase(form.getBatchCode().trim())))throw new IllegalStateException("Mã lô đã tồn tại cho quy cách này.");
  ProductBatch b=new ProductBatch();b.setProduct(p);b.setBatchCode(form.getBatchCode().trim());b.setManufacturedOn(form.getManufacturedOn());b.setExpiresOn(form.getExpiresOn());b.setRemainingQuantity(form.getRemainingQuantity());batches.save(b);p.setBatchTracked(true);products.save(p);
  return "redirect:/admin/products?edit="+id;
 }
 @PostMapping("/{id}/batches/{batchId}/stock") @Transactional
 public String stock(@PathVariable Long id,@PathVariable Long batchId,@RequestParam Double remainingQuantity){
  products.lockById(id).orElseThrow();ProductBatch b=batches.findById(batchId).orElseThrow();if(!b.getProduct().getId().equals(id))throw new IllegalStateException("Lô không thuộc sản phẩm.");validateQuantity(remainingQuantity);b.setRemainingQuantity(remainingQuantity);batches.save(b);return "redirect:/admin/products?edit="+id;
 }
 static void validateQuantity(Double q){if(q==null||!Double.isFinite(q)||q<0||java.math.BigDecimal.valueOf(q).stripTrailingZeros().scale()>2)throw new IllegalStateException("Tồn lô phải không âm, tối đa 2 chữ số thập phân.");}
 @ExceptionHandler(IllegalStateException.class) public String invalid(IllegalStateException e,RedirectAttributes f){f.addFlashAttribute("error",e.getMessage());return "redirect:/admin/products";}
}
