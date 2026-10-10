package vn.edu.crs.tinhhoataybac;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.crs.tinhhoataybac.model.*;
import vn.edu.crs.tinhhoataybac.repository.*;
import vn.edu.crs.tinhhoataybac.service.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @ActiveProfiles("test") @Transactional
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,TransactionalTestExecutionListener.class})
class FoodInventoryIntegrationTests{
 @Autowired ProductRepository products; @Autowired ProductBatchRepository batches;
 @Autowired OrderService orders; @Autowired CommerceService commerce;
 @Autowired UserRepository users;
 @Autowired org.springframework.web.context.WebApplicationContext context;
 @Test void purchaseAcrossLotsThenCancelRestoresExactLots(){
  Product p=new Product();p.setName("FOOD TEST");p.setPrice(35000d);p.setStock(100d);p.setBatchTracked(true);p.setVariantLabel("Gói 500 g");products.saveAndFlush(p);
  for(int n=1;n<=2;n++){ProductBatch b=new ProductBatch();b.setProduct(p);b.setBatchCode("LOT"+n);b.setManufacturedOn(LocalDate.now().minusDays(1));b.setExpiresOn(LocalDate.now().plusDays(n*10));b.setRemainingQuantity(n==1?1d:3d);batches.save(b);p.getBatches().add(b);}
  User u=new User();u.setEmail("food-inventory@test.invalid");u.setFullName("TEST");u.setPassword("unused");users.save(u);
  var o=orders.createOrder("TEST","0000000000",u.getEmail(),"TEST ONLY","TEST ONLY","COD",java.util.List.of(new CartItem(p,2)));
  assertEquals(new BigDecimal("70000.00"),o.getTotalAmount().setScale(2));assertEquals(2,o.getOrderDetails().size());assertEquals("LOT1",o.getOrderDetails().get(0).getBatchCodeSnapshot());assertEquals("Gói 500 g",o.getOrderDetails().get(0).getVariantSnapshot());assertEquals(2,p.getStock());
  commerce.changeStatus(o.getId(),"CANCELLED","TEST");assertEquals(4,p.getStock());assertEquals(1,p.getBatches().get(0).getRemainingQuantity());assertEquals(3,p.getBatches().get(1).getRemainingQuantity());
 }
 @Test void adminAndBuyerTemplatesRenderAndSaveFoodProfile() throws Exception{
  User admin=new User();admin.setEmail("food-admin@test.invalid");admin.setFullName("ADMIN TEST");admin.setPassword("unused");admin.setRole("ADMIN");users.save(admin);
  Product p=new Product();p.setName("FOOD TEST");p.setPrice(35000d);p.setStock(5d);p.setVariantLabel("Gói 500 g");p.setFamilyCode("FOOD-TEST");products.saveAndFlush(p);
  var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/admin/products").param("edit",p.getId().toString()).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(admin.getEmail()).roles("ADMIN"))).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/products/save").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(admin.getEmail()).roles("ADMIN")).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).param("id",p.getId().toString()).param("name","FOOD TEST").param("price","35000").param("stock","5").param("minQuantity","1").param("quantityStep","1").param("familyCode","FOOD-TEST").param("variantLabel","Gói 500 g").param("ingredients","Thành phần kiểm thử").param("storageInstructions","Bảo quản kiểm thử")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is3xxRedirection());
  assertEquals("Thành phần kiểm thử",products.findById(p.getId()).orElseThrow().getIngredients());
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/products/"+p.getId())).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
 }
}
