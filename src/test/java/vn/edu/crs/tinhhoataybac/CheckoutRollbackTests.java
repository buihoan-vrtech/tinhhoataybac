package vn.edu.crs.tinhhoataybac;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import vn.edu.crs.tinhhoataybac.model.*;
import vn.edu.crs.tinhhoataybac.repository.*;
import vn.edu.crs.tinhhoataybac.service.*;

@SpringBootTest
@ActiveProfiles("test")
class CheckoutRollbackTests {
  @Autowired CommerceService commerce;
  @Autowired ProductRepository products;
  @Autowired UserRepository users;
  @Autowired OrderRepository orders;

  @Test
  void failedWalletPaymentRollsBackOrderAndStock() {
    User u = new User();
    u.setEmail("rollback@test.local");
    u.setFullName("Test");
    u.setPassword("unused");
    users.save(u);
    Product p = new Product();
    p.setName("Test");
    p.setPrice(100000.0);
    p.setStock(5.0);
    products.save(p);
    long count = orders.count();
    assertThrows(
        IllegalStateException.class,
        () ->
            commerce.checkout(
                u,
                "Test",
                "0901234567",
                "Test address",
                "",
                "WALLET",
                List.of(new CartItem(p, 1)),
                "standard",
                null));
    assertEquals(count, orders.count());
    assertEquals(5.0, products.findById(p.getId()).orElseThrow().getStock());
    products.deleteById(p.getId());
    users.deleteById(u.getId());
  }
}
