package vn.edu.crs.tinhhoataybac;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.mock.web.MockHttpSession;
import vn.edu.crs.tinhhoataybac.model.*;
import vn.edu.crs.tinhhoataybac.repository.*;
import vn.edu.crs.tinhhoataybac.service.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest @ActiveProfiles("test") @Transactional
@TestPropertySource(properties={"sepay.webhook-secret=integration-test-only","payment.qr.account-no=TEST-ACCOUNT"})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,TransactionalTestExecutionListener.class})
class OwnershipCartPaymentTests {
 @Autowired ProductBatchRepository batches; @Autowired UserRepository users; @Autowired ProductRepository products; @Autowired OrderRepository repository;
 @Autowired OrderService orders; @Autowired CommerceService commerce; @Autowired CartService cart;
 @Autowired WebApplicationContext context; @Autowired jakarta.persistence.EntityManager em;
 User customer(String name){var u=new User();u.setEmail(name+"@test.invalid");u.setFullName("TEST "+name);u.setPassword("unused");u.setRole("USER");return users.saveAndFlush(u);}
 Product product(){var p=new Product();p.setName("TEST PRODUCT");p.setPrice(10000d);p.setStock(3d);return products.saveAndFlush(p);}
 Order order(User u,Product p){return commerce.checkout(u,"TEST","0000000000","TEST ONLY",null,"QR",java.util.List.of(new CartItem(p,1)),"standard",null);}
 org.springframework.test.web.servlet.MockMvc mvc(){return org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();}
 @Test void customerIdWinsOverEmailForListsAndDetails(){var owner=customer("owner");var other=customer("other");var o=order(owner,product());o.setEmail(other.getEmail());repository.saveAndFlush(o);assertEquals(owner.getId(),o.getCustomer().getId());assertTrue(orders.getOrdersByEmail(owner.getEmail()).stream().anyMatch(x->x.getId().equals(o.getId())));assertTrue(orders.getOrdersByEmail(other.getEmail()).isEmpty());assertNotNull(orders.getOrderForUser(o.getId(),owner.getEmail()));assertNull(orders.getOrderForUser(o.getId(),other.getEmail()));}
 @Test void strangerCannotReadQrSuccessDetailOrStatus() throws Exception{var owner=customer("private-owner");var other=customer("stranger");var o=order(owner,product());for(var path:java.util.List.of("/payment/qr/","/payment/wait/","/order-success/","/account/orders/")){mvc().perform(get(path+o.getId()).with(user(other.getEmail()))).andExpect(status().is3xxRedirection()).andExpect(content().string(""));}mvc().perform(get("/api/payment/status/"+o.getId()).with(user(other.getEmail()))).andExpect(status().isNotFound());}
 @Test void unpaidQrCannotShowSuccessAndLegacyCompletionDoesNotPay() throws Exception{var u=customer("pending");var o=order(u,product());mvc().perform(get("/order-success/"+o.getId()).with(user(u.getEmail()))).andExpect(redirectedUrl("/payment/qr/"+o.getId()));mvc().perform(get("/payment/qr/complete").param("orderId",o.getId().toString()).with(user(u.getEmail()))).andExpect(status().is4xxClientError());assertEquals("PENDING",repository.findById(o.getId()).orElseThrow().getPaymentStatus());}
 @Test void cartRefreshesPriceAndRejectsZeroOrExcessStock(){var p=product();var session=new MockHttpSession();cart.addToCart(session,p,1);products.findById(p.getId()).orElseThrow().setPrice(25000d);products.flush();em.clear();assertEquals(25000d,cart.getTotal(session));var fresh=products.findById(p.getId()).orElseThrow();assertThrows(IllegalStateException.class,()->cart.addToCart(session,fresh,3));assertEquals(1,cart.getCart(session).get(0).getQuantity());fresh.setStock(0d);products.saveAndFlush(fresh);assertThrows(IllegalStateException.class,()->cart.addToCart(new MockHttpSession(),fresh,1));}
 @Test void webhookNeedsAuthenticationAccountAndExactAmount() throws Exception{var u=customer("webhook");var o=order(u,product());String prefix="{\"id\":987654,\"transferType\":\"in\",\"code\":\""+o.getPaymentCode()+"\",\"accountNumber\":\"";var m=mvc();String payload=prefix+"TEST-ACCOUNT\",\"transferAmount\":"+o.getTotalAmount()+"}";m.perform(post("/api/sepay/webhook").contentType("application/json").content(payload)).andExpect(status().isUnauthorized());m.perform(post("/api/sepay/webhook").header("Authorization","Apikey integration-test-only").contentType("application/json").content(prefix+"WRONG\",\"transferAmount\":"+o.getTotalAmount()+"}")).andExpect(status().isBadRequest());m.perform(post("/api/sepay/webhook").header("Authorization","Apikey integration-test-only").contentType("application/json").content(prefix+"TEST-ACCOUNT\",\"transferAmount\":1}")).andExpect(status().isOk());assertEquals("PENDING",o.getPaymentStatus());m.perform(post("/api/sepay/webhook").header("Authorization","Apikey integration-test-only").contentType("application/json").content(payload)).andExpect(status().isOk());assertEquals("PAID",o.getPaymentStatus());m.perform(get("/api/payment/status/"+o.getId()).with(user(u.getEmail()))).andExpect(jsonPath("$.paid").value(true));}
 @Test void customerCancelRestoresExactBatchOnceAndRejectsOtherUser() throws Exception {
  var u=customer("cancel-owner");var other=customer("cancel-other");var p=product();p.setBatchTracked(true);
  var batch=new ProductBatch();batch.setProduct(p);batch.setBatchCode("CANCEL-TEST");batch.setManufacturedOn(java.time.LocalDate.now().minusDays(1));batch.setExpiresOn(java.time.LocalDate.now().plusDays(30));batch.setRemainingQuantity(3d);batches.saveAndFlush(batch);p.getBatches().add(batch);products.saveAndFlush(p);
  var o=order(u,p);assertEquals(2d,p.getStock());
  assertThrows(IllegalStateException.class,()->commerce.cancelByUser(o.getId(),other,"TEST"));assertEquals(2d,p.getStock());
  mvc().perform(post("/account/orders/"+o.getId()+"/cancel").with(user(u.getEmail())).with(csrf()).param("reason","TEST ONLY")).andExpect(status().is3xxRedirection());
  assertEquals("CANCELLED",o.getOrderStatus());assertEquals(3d,p.getStock());
  assertThrows(IllegalStateException.class,()->commerce.cancelByUser(o.getId(),u,"TEST"));assertEquals(3d,p.getStock());
 }
}
