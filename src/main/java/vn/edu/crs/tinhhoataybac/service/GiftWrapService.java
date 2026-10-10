package vn.edu.crs.tinhhoataybac.service;
import org.springframework.stereotype.Service;
import vn.edu.crs.tinhhoataybac.repository.GiftWrapOptionRepository;
import vn.edu.crs.tinhhoataybac.model.GiftWrapOption;
@Service public class GiftWrapService{
 private final GiftWrapOptionRepository options;
 public GiftWrapService(GiftWrapOptionRepository o){options=o;}
 public java.util.List<GiftWrapOption> active(){return options.findByActiveTrueOrderByIdAsc();}
 public GiftWrapOption selected(Long id){if(id==null)return null;return options.findById(id).filter(GiftWrapOption::isActive).orElseThrow(()->new IllegalStateException("Hộp quà không còn khả dụng. Vui lòng chọn lại."));}
 public java.math.BigDecimal fee(Long id){var o=selected(id);return o==null?java.math.BigDecimal.ZERO:o.getPrice();}
 public static String message(String s){if(s==null)return null;String v=s.trim();if(v.length()>500)throw new IllegalStateException("Lời chúc tối đa 500 ký tự.");return v.isBlank()?null:v;}
}
