package vn.edu.crs.tinhhoataybac.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpSession;
import vn.edu.crs.tinhhoataybac.repository.ProductBundleRepository;
import vn.edu.crs.tinhhoataybac.model.CartItem;
@Service public class BundleCartService{
 private final ProductBundleRepository bundles;private final CartService cart;
 public BundleCartService(ProductBundleRepository b,CartService c){bundles=b;cart=c;}
 @Transactional(readOnly=true) public void add(Long id,HttpSession session){
  var b=bundles.findById(id).filter(v->v.isActive()&&!v.getComponents().isEmpty()).orElseThrow(()->new IllegalStateException("Combo không còn khả dụng."));
  synchronized(session){
   var current=cart.getCart(session);
   for(var c:b.getComponents()){var p=c.getProduct();p.validateQuantity(c.getQuantity());double existing=current.stream().filter(i->i.getProduct().getId().equals(p.getId())).mapToDouble(CartItem::getQuantity).sum();if(p.getStock()==null||existing+c.getQuantity()>p.getStock())throw new IllegalStateException("Không đủ hàng cho combo: "+p.getName());}
   for(var c:b.getComponents())cart.addToCart(session,c.getProduct(),c.getQuantity());
  }
 }
}
