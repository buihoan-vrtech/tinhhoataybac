package vn.edu.crs.tinhhoataybac;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest @ActiveProfiles("test")
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
class AdminShoppingAccessTests {
 @Autowired WebApplicationContext context;
 @Test void adminCannotOpenCustomerPurchaseEndpoints() throws Exception {
  var mvc=webAppContextSetup(context).apply(springSecurity()).build();
  for(var path:java.util.List.of("/cart","/checkout","/payment/qr/1","/wallet","/api/payment/status/1","/order-success/1"))
   mvc.perform(get(path).with(user("admin-test").roles("ADMIN"))).andExpect(status().isForbidden());
 }
 @Test void adminCannotMutateCartOrPlaceOrdersEvenWithValidCsrf() throws Exception {
  var mvc=webAppContextSetup(context).apply(springSecurity()).build();
  for(var path:java.util.List.of("/cart/add","/cart/update","/cart/remove","/cart/clear","/combos/1/add","/checkout/quote","/checkout/place-order"))
   mvc.perform(post(path).with(user("admin-test").roles("ADMIN")).with(csrf())).andExpect(status().isForbidden());
 }
 @Test void adminWithBothRolesIsStillBlockedAndGuestMustLogin() throws Exception {
  var mvc=webAppContextSetup(context).apply(springSecurity()).build();
  mvc.perform(get("/cart").with(user("admin-test").roles("ADMIN","USER"))).andExpect(status().isForbidden());
  mvc.perform(get("/cart")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/cart").with(user("customer-test").roles("USER"))).andExpect(status().isOk());
 }
}
