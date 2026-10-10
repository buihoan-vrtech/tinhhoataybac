package vn.edu.crs.tinhhoataybac;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.WebApplicationContext;
import vn.edu.crs.tinhhoataybac.model.*;
import vn.edu.crs.tinhhoataybac.repository.*;
import vn.edu.crs.tinhhoataybac.service.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest @ActiveProfiles("test") @Transactional
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,TransactionalTestExecutionListener.class})
class GiftCommerceIntegrationTests{
 @Autowired ProductRepository products;@Autowired ProductBundleRepository bundles;@Autowired GiftWrapOptionRepository wraps;@Autowired UserRepository users;
 @Autowired CartService cart;@Autowired BundleCartService combo;@Autowired CommerceService commerce;@Autowired WebApplicationContext context;
 Product product(String name,double price,double stock){var p=new Product();p.setName(name);p.setPrice(price);p.setStock(stock);products.save(p);return p;}
 ProductBundle bundle(Product a,Product b){var v=new ProductBundle();v.setName("TEST COMBO");v.setActive(true);for(var p:java.util.List.of(a,b)){var c=new BundleComponent();c.setBundle(v);c.setProduct(p);c.setQuantity(1);v.getComponents().add(c);}return bundles.saveAndFlush(v);}
 GiftWrapOption wrap(){var w=new GiftWrapOption();w.setName("TEST GIFT");w.setPrice(new BigDecimal("20000"));w.setActive(true);return wraps.save(w);}
 User customer(){var u=new User();u.setEmail("gift-test@localhost.invalid");u.setFullName("TEST");u.setPassword("unused");return users.save(u);}
 @Test void giftFeeMatchesQuoteOrderAndQrAndSnapshotDoesNotChange(){var a=product("A",10000,3);var b=product("B",20000,3);var bundle=bundle(a,b);var wrap=wrap();var u=customer();var s=new MockHttpSession();combo.add(bundle.getId(),s);assertEquals(2,cart.getCart(s).size());var q=commerce.quote(cart.getCart(s),"standard",null,wrap.getId());assertEquals(0,new BigDecimal("75000").compareTo(q.get("total")));var o=commerce.checkout(u,"TEST","0000000000","TEST ONLY","TEST ONLY","QR",cart.getCart(s),"standard",null,wrap.getId(),"  Chúc vui vẻ  ",true);assertEquals(0,q.get("total").compareTo(o.getTotalAmount()));assertEquals("Chúc vui vẻ",o.getGiftMessage());assertTrue(o.getHidePrices());assertEquals("TEST GIFT",o.getGiftWrapName());wrap.setName("CHANGED");wrap.setPrice(new BigDecimal("40000"));wraps.save(wrap);assertEquals("TEST GIFT",o.getGiftWrapName());assertEquals(0,new BigDecimal("20000").compareTo(o.getGiftWrapFee()));assertTrue(new BankQrService("MB","0000000000","TEST").url(o).contains("amount=75000"));}
 @Test void unavailableComboDoesNotAddPartialContents(){var a=product("A",10000,2);var b=product("B",20000,0);var bundle=bundle(a,b);var s=new MockHttpSession();cart.addToCart(s,a,1);assertThrows(IllegalStateException.class,()->combo.add(bundle.getId(),s));assertEquals(1,cart.getCart(s).size());assertEquals(1,cart.getCart(s).get(0).getQuantity());b.setStock(2d);products.save(b);a.setStock(1d);products.save(a);assertThrows(IllegalStateException.class,()->combo.add(bundle.getId(),s));assertEquals(1,cart.getCart(s).size());}
 @Test void rejectsInactiveWrapAndOversizedGreetingBeforeStockReservation(){var p=product("A",10000,3);var w=wrap();w.setActive(false);wraps.save(w);var u=customer();assertThrows(IllegalStateException.class,()->commerce.checkout(u,"TEST","0000000000","TEST ONLY",null,"COD",java.util.List.of(new CartItem(p,1)),"standard",null,w.getId(),null,false));assertEquals(3,p.getStock());assertThrows(IllegalStateException.class,()->GiftWrapService.message("x".repeat(501)));}
 @Test void adminCanCreateAndEditComboAndGiftOptions() throws Exception{
  var a=product("A",10000,3);var b=product("B",20000,3);var u=customer();var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
  mvc.perform(post("/admin/gifts/bundles/save").with(user(u.getEmail()).roles("ADMIN")).with(csrf()).param("name","ADMIN COMBO TEST").param("active","true").param("productIds",a.getId().toString(),b.getId().toString(),"").param("quantities","1","1","1")).andExpect(status().is3xxRedirection());
  var saved=bundles.findAll().stream().filter(v->v.getName().equals("ADMIN COMBO TEST")).findFirst().orElseThrow();assertEquals(2,saved.getComponents().size());
  mvc.perform(post("/admin/gifts/bundles/save").with(user(u.getEmail()).roles("ADMIN")).with(csrf()).param("id",saved.getId().toString()).param("name","ADMIN COMBO TEST").param("productIds",a.getId().toString(),b.getId().toString()).param("quantities","2","1")).andExpect(status().is3xxRedirection());assertFalse(saved.isActive());assertEquals(2,saved.getComponents().size());assertEquals(2,saved.getComponents().get(0).getQuantity());
  mvc.perform(post("/admin/gifts/wraps/save").with(user(u.getEmail()).roles("ADMIN")).with(csrf()).param("name","ADMIN GIFT TEST").param("price","20000").param("active","true")).andExpect(status().is3xxRedirection());assertTrue(wraps.findByActiveTrueOrderByIdAsc().stream().anyMatch(v->v.getName().equals("ADMIN GIFT TEST")));
 }
 @Test void catalogueAdminCheckoutAndOrderTemplatesRender() throws Exception{var a=product("A",10000,3);var b=product("B",20000,3);var v=bundle(a,b);var w=wrap();var u=customer();var s=new MockHttpSession();combo.add(v.getId(),s);var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();mvc.perform(get("/combos")).andExpect(status().isOk());mvc.perform(get("/admin/gifts").param("edit",v.getId().toString()).with(user(u.getEmail()).roles("ADMIN"))).andExpect(status().isOk());mvc.perform(get("/checkout").session(s).with(user(u.getEmail()))).andExpect(status().isOk());var o=commerce.checkout(u,"TEST","0000000000","TEST ONLY",null,"QR",cart.getCart(s),"standard",null,w.getId(),"TEST",true);mvc.perform(get("/payment/qr/"+o.getId()).with(user(u.getEmail()))).andExpect(status().isOk());mvc.perform(get("/account/orders/"+o.getId()).with(user(u.getEmail()))).andExpect(status().isOk());mvc.perform(get("/admin/orders/"+o.getId()).with(user(u.getEmail()).roles("ADMIN"))).andExpect(status().isOk());}
}


