package vn.edu.crs.tinhhoataybac;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import vn.edu.crs.tinhhoataybac.model.*;
import vn.edu.crs.tinhhoataybac.model.Order;
import vn.edu.crs.tinhhoataybac.repository.*;
import vn.edu.crs.tinhhoataybac.service.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommerceFeaturesTests {
  @Autowired AdminCustomerService adminCustomers;
  @Autowired WalletTopUpService topUps;
  @Autowired WalletTopUpRepository topUpRepo;
  @Autowired ShipmentEventRepository shipmentEvents;
  @Autowired CommerceService commerce;
  @Autowired OrderService orderService;
  @Autowired VoucherService voucherService;
  @Autowired WalletService wallets;
  @Autowired ProductRepository products;
  @Autowired UserRepository users;
  @Autowired OrderRepository orders;
  @Autowired VoucherRepository vouchers;
  @Autowired OrderServiceRequestRepository requests;
  @Autowired UserAddressRepository addresses;
  @Autowired ReviewRepository reviews;
  @Autowired EmailChallengeRepository challenges;
  @Autowired EmailChallengeService emailCodes;
  @Autowired PasswordEncoder encoder;
  @Autowired WebApplicationContext context;
  private User customer, other;
  private Product product;
  private MockMvc mvc;

  @BeforeEach
  void fixtures() {
    customer = new User();
    customer.setFullName("Khách kiểm thử");
    customer.setEmail("customer@test.local");
    customer.setPassword(encoder.encode("TestPass123"));
    users.save(customer);
    other = new User();
    other.setFullName("Khách khác");
    other.setEmail("other@test.local");
    other.setPassword(encoder.encode("TestPass123"));
    users.save(other);
    product = new Product();
    product.setName("Thịt trâu gác bếp");
    product.setPrice(200000.0);
    product.setStock(10.0);
    product.setUnit("kg");
    product.setMinQuantity(0.5);
    product.setQuantityStep(0.25);
    product.setImage("/images/test.png");
    products.save(product);
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  private Order place(String method, String voucher) {
    return commerce.checkout(
        customer,
        "Khách",
        "0901234567",
        "Địa chỉ thử",
        "",
        method,
        List.of(new CartItem(product, 1)),
        "standard",
        voucher);
  }

  @Test
  void voucherCalculationAndExpiry() {
    Voucher v = new Voucher();
    v.setType("percent");
    v.setValue(new BigDecimal("20"));
    v.setMaxDiscount(new BigDecimal("15000"));
    assertEquals(
        0,
        voucherService
            .discount(v, new BigDecimal("200000"), new BigDecimal("25000"))
            .compareTo(new BigDecimal("15000")));
    v.setExpiresAt(LocalDateTime.now().minusSeconds(1));
    assertThrows(
        IllegalStateException.class,
        () -> voucherService.discount(v, new BigDecimal("200000"), BigDecimal.ZERO));
  }

  @Test
  void promotionsAndFractionalQuantities() {
    product.setPromotionPrice(150000.0);
    product.setPromotionStartsAt(LocalDateTime.now().minusHours(1));
    product.setPromotionExpiresAt(LocalDateTime.now().plusHours(1));
    products.save(product);
    Order o =
        commerce.checkout(
            customer,
            "Khách",
            "0901234567",
            "Địa chỉ thử",
            "",
            "COD",
            List.of(new CartItem(product, 0.75)),
            "fast",
            null);
    assertEquals(0, o.getTotalAmount().compareTo(new BigDecimal("147500")));
    assertEquals(9.25, products.findById(product.getId()).orElseThrow().getStock());
    assertThrows(IllegalStateException.class, () -> product.validateQuantity(0.6));
  }

  @Test
  void paidCancellationRefundsAndRestoresInventoryOnce() {
    wallets.deposit(customer, new BigDecimal("500000"), "Thử", null);
    Voucher v = new Voucher();
    v.setCode("TEST10");
    v.setType("fixed");
    v.setValue(new BigDecimal("10000"));
    vouchers.save(v);
    Order o = place("WALLET", "TEST10");
    assertEquals("PAID", o.getPaymentStatus());
    assertEquals(0, wallets.getBalance(customer).compareTo(new BigDecimal("285000")));
    commerce.changeStatus(o.getId(), "CANCELLED", "admin");
    commerce.changeStatus(o.getId(), "CANCELLED", "admin");
    assertEquals("REFUNDED", o.getPaymentStatus());
    assertEquals(0, wallets.getBalance(customer).compareTo(new BigDecimal("500000")));
    assertEquals(10.0, products.findById(product.getId()).orElseThrow().getStock());
    assertEquals(0, vouchers.findById(v.getId()).orElseThrow().getUsedCount());
  }

  @Test
  void cannotSkipStatusOrReviveCancelledOrder() {
    Order o = place("COD", null);
    assertThrows(
        IllegalStateException.class, () -> commerce.changeStatus(o.getId(), "COMPLETED", "admin"));
    commerce.changeStatus(o.getId(), "CANCELLED", "admin");
    assertThrows(
        IllegalStateException.class, () -> commerce.changeStatus(o.getId(), "CONFIRMED", "admin"));
    assertFalse(orderService.markOrderPaid(o.getPaymentCode(), o.getTotalAmount()));
  }

  @Test
  void lateQrCannotReviveExpiredOrder() {
    Order o = place("QR", null);
    o.setPaymentExpiresAt(LocalDateTime.now().minusMinutes(1));
    orders.save(o);
    assertFalse(orderService.markOrderPaid(o.getPaymentCode(), o.getTotalAmount()));
    commerce.expirePendingPayments();
    assertEquals("CANCELLED", orders.findById(o.getId()).orElseThrow().getOrderStatus());
    assertEquals(10.0, products.findById(product.getId()).orElseThrow().getStock());
  }

  @Test
  void refundRequestIsProcessedOnce() {
    Order o = place("COD", null);
    commerce.changeStatus(o.getId(), "CONFIRMED", "admin");
    commerce.changeStatus(o.getId(), "SHIPPING", "admin");
    commerce.changeStatus(o.getId(), "COMPLETED", "admin");
    commerce.request(o.getId(), customer, "refund", "Sản phẩm có vấn đề chất lượng");
    var r = requests.findByOrderIdOrderByCreatedAtDesc(o.getId()).getFirst();
    commerce.processRequest(r.getId(), "APPROVED", "Đã kiểm tra", "admin");
    assertEquals("REFUNDED", o.getPaymentStatus());
    assertThrows(
        IllegalStateException.class,
        () -> commerce.processRequest(r.getId(), "APPROVED", "", "admin"));
    assertEquals(0, wallets.getBalance(customer).compareTo(o.getTotalAmount()));
  }

  @Test
  void ownershipAndAuthorization() throws Exception {
    Order o = place("QR", null);
    assertFalse(commerce.owns(o, other));
    mvc.perform(get("/payment/qr/" + o.getId()).with(user(other.getEmail())))
        .andExpect(status().is3xxRedirection());
    mvc.perform(get("/api/payment/status/" + o.getId()).with(user(other.getEmail())))
        .andExpect(status().isNotFound());
    mvc.perform(get("/admin/products").with(user(customer.getEmail())))
        .andExpect(status().isForbidden());
    mvc.perform(post("/products/" + product.getId() + "/reviews").with(csrf()).param("rating", "5"))
        .andExpect(status().is3xxRedirection());
  }

  @Test
  void renderAllFeaturePages() throws Exception {
    Order o = place("COD", null);
    for (String path :
        List.of(
            "/admin/dashboard",
            "/admin/products",
            "/admin/products?edit=" + product.getId(),
            "/admin/categories",
            "/admin/vouchers",
            "/admin/orders",
            "/admin/orders/" + o.getId(),
            "/admin/service-requests",
            "/admin/wallets",
            "/account",
            "/account/profile",
            "/account/addresses",
            "/account/notifications",
            "/account/orders",
            "/account/orders/" + o.getId(),
            "/products/" + product.getId(),
            "/khuyen-mai",
            "/forgot-password",
            "/chinh-sach-giao-hang",
            "/chinh-sach-doi-tra",
            "/chinh-sach-bao-mat",
            "/dieu-khoan-dich-vu",
            "/chinh-sach-thanh-toan")) {
      mvc.perform(get(path).with(user(customer.getEmail()).roles("ADMIN")))
          .andExpect(status().isOk());
    }
  }

  @Test
  void checkoutRendersSavedAddresses() throws Exception {
    UserAddress address = new UserAddress();
    address.setUser(customer);
    address.setReceiverName("Khách");
    address.setPhone("0901234567");
    address.setAddressDetail("Địa chỉ thử");
    addresses.save(address);
    var session = new org.springframework.mock.web.MockHttpSession();
    session.setAttribute("cart", new ArrayList<>(List.of(new CartItem(product, 1))));
    mvc.perform(get("/checkout").session(session).with(user(customer.getEmail())))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("saved-address")));
  }

  @Test
  void reviewsRequireDeliveredPurchase() throws Exception {
    mvc.perform(
            post("/products/" + product.getId() + "/reviews")
                .with(user(customer.getEmail()))
                .with(csrf())
                .param("rating", "5"))
        .andExpect(status().is3xxRedirection());
    assertTrue(reviews.findByProductIdOrderByCreatedAtDesc(product.getId()).isEmpty());
    Order o = place("COD", null);
    commerce.changeStatus(o.getId(), "CONFIRMED", "admin");
    commerce.changeStatus(o.getId(), "SHIPPING", "admin");
    commerce.changeStatus(o.getId(), "COMPLETED", "admin");
    mvc.perform(
            post("/products/" + product.getId() + "/reviews")
                .with(user(customer.getEmail()))
                .with(csrf())
                .param("rating", "5")
                .param("comment", "Rất ngon"))
        .andExpect(status().is3xxRedirection());
    assertEquals(1, reviews.findByProductIdOrderByCreatedAtDesc(product.getId()).size());
  }

  @Test
  void otpAttemptsAndSingleUse() {
    EmailChallenge e = new EmailChallenge();
    e.setUser(customer);
    e.setPurpose("reset");
    e.setCodeHash(encoder.encode("123456"));
    e.setExpiresAt(LocalDateTime.now().plusMinutes(10));
    e.setSentAt(LocalDateTime.now());
    challenges.save(e);
    assertNotNull(emailCodes.confirm(customer.getEmail(), "reset", "000000", "NewPass123"));
    assertEquals(1, e.getAttempts());
    assertNull(emailCodes.confirm(customer.getEmail(), "reset", "123456", "NewPass123"));
    assertTrue(
        encoder.matches(
            "NewPass123", users.findById(customer.getId()).orElseThrow().getPassword()));
    assertNotNull(emailCodes.confirm(customer.getEmail(), "reset", "123456", "Again123"));
  }

  @Test
  void adminFormsSaveBoundValues() throws Exception {
    mvc.perform(
            post("/admin/vouchers/save")
                .with(user(customer.getEmail()).roles("ADMIN"))
                .with(csrf())
                .param("code", "BOUND20")
                .param("name", "Voucher test")
                .param("type", "percent")
                .param("value", "20")
                .param("minOrderValue", "10000")
                .param("active", "true")
                .param("startsAt", "2026-01-01T12:00")
                .param("expiresAt", "2027-01-01T12:00"))
        .andExpect(status().is3xxRedirection());
    Voucher v = vouchers.findByCodeIgnoreCase("BOUND20").orElseThrow();
    assertEquals(0, v.getValue().compareTo(new BigDecimal("20")));
    assertNotNull(v.getExpiresAt());
    mvc.perform(
            post("/admin/products/save")
                .with(user(customer.getEmail()).roles("ADMIN"))
                .with(csrf())
                .param("id", product.getId().toString())
                .param("name", "Updated product")
                .param("price", "210000")
                .param("stock", "9.5")
                .param("unit", "kg")
                .param("minQuantity", "0.5")
                .param("quantityStep", "0.25")
                .param("promotionPrice", "180000")
                .param("promotionStartsAt", "2026-01-01T12:00")
                .param("promotionExpiresAt", "2027-01-01T12:00"))
        .andExpect(status().is3xxRedirection());
    var updated = products.findById(product.getId()).orElseThrow();
    assertEquals("Updated product", updated.getName());
    assertEquals(180000.0, updated.getEffectivePrice());
    assertEquals(9.5, updated.getStock());
  }

  @Test
  void quoteMatchesOrderWithoutConsumingVoucher() throws Exception {
    Voucher v = new Voucher();
    v.setCode("QUOTE10");
    v.setType("fixed");
    v.setValue(new BigDecimal("10000"));
    vouchers.save(v);
    var q = commerce.quote(List.of(new CartItem(product, 1)), "fast", "QUOTE10");
    assertEquals(0, q.get("total").compareTo(new BigDecimal("225000")));
    assertEquals(0, v.getUsedCount());
    var session = new org.springframework.mock.web.MockHttpSession();
    session.setAttribute("cart", new ArrayList<>(List.of(new CartItem(product, 1))));
    mvc.perform(
            post("/checkout/quote")
                .session(session)
                .with(user(customer.getEmail()))
                .with(csrf())
                .param("shippingMethod", "fast")
                .param("voucherCode", "QUOTE10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(225000));
  }

  @Test
  void customerAddressAndProfileFormsPersist() throws Exception {
    mvc.perform(
            post("/account/addresses/save")
                .with(user(customer.getEmail()))
                .with(csrf())
                .param("receiverName", "Receiver")
                .param("phone", "0901234567")
                .param("addressDetail", "New address")
                .param("label", "Home")
                .param("defaultAddress", "true"))
        .andExpect(status().is3xxRedirection());
    var saved = addresses.findByUserIdOrderByDefaultAddressDescIdDesc(customer.getId()).getFirst();
    assertTrue(saved.getDefaultAddress());
    assertEquals("Receiver", saved.getReceiverName());
    mvc.perform(
            post("/account/profile")
                .with(user(customer.getEmail()))
                .with(csrf())
                .param("fullName", "Updated Name")
                .param("phone", "0901234567")
                .param("address", "New address"))
        .andExpect(status().is3xxRedirection());
    assertEquals("Updated Name", users.findById(customer.getId()).orElseThrow().getFullName());
  }

  @Test
  void manualPaymentAndShipmentAreRecorded() {
    Order o = place("QR", null);
    assertThrows(
        IllegalStateException.class,
        () -> commerce.confirmManualPayment(o.getId(), false, "admin"));
    commerce.confirmManualPayment(o.getId(), true, "admin");
    assertEquals("PAID", o.getPaymentStatus());
    commerce.changeStatus(o.getId(), "CONFIRMED", "admin");
    commerce.changeStatus(o.getId(), "SHIPPING", "admin");
    commerce.shipmentDetails(
        o.getId(),
        "GHN",
        "TEST123",
        "in_transit",
        LocalDate.now().plusDays(2),
        "Hanoi",
        "Test shipment",
        "admin");
    assertEquals(1, shipmentEvents.findByOrderIdOrderByCreatedAtDesc(o.getId()).size());
    assertEquals("in_transit", o.getShipmentStatus());
  }

  @Test
  void adminWalletAdjustmentCannotOverdraw() {
    adminCustomers.adjust(
        customer.getId(), "credit", new BigDecimal("50000"), "Test credit", "admin");
    assertThrows(
        IllegalStateException.class,
        () ->
            adminCustomers.adjust(
                customer.getId(), "debit", new BigDecimal("60000"), "Test debit", "admin"));
    assertEquals(
        0, wallets.getOrCreateWallet(customer).getBalance().compareTo(new BigDecimal("50000")));
  }

  @Test
  void newAdminPagesRender() throws Exception {
    for (String path :
        List.of("/admin/customers", "/admin/customers/" + customer.getId(), "/admin/reviews"))
      mvc.perform(get(path).with(user(customer.getEmail()).roles("ADMIN")))
          .andExpect(status().isOk());
    mvc.perform(get("/categories")).andExpect(status().isOk());
  }

  @Test
  void topUpReviewPreventsDuplicateCreditAndRejectedCallbacks() {
    WalletTopUp t = topUps.createTopUp(customer, new BigDecimal("50000"));
    adminCustomers.reviewTopUp(customer.getId(), t.getId(), "approve", "Received", true, "admin");
    assertTrue(topUps.confirmTopUp(t.getPaymentCode(), t.getAmount()));
    assertEquals(
        0, wallets.getOrCreateWallet(customer).getBalance().compareTo(new BigDecimal("50000")));
    WalletTopUp rejected = topUps.createTopUp(customer, new BigDecimal("20000"));
    adminCustomers.reviewTopUp(
        customer.getId(), rejected.getId(), "reject", "No transfer", false, "admin");
    assertFalse(topUps.confirmTopUp(rejected.getPaymentCode(), rejected.getAmount()));
    assertEquals(
        0, wallets.getOrCreateWallet(customer).getBalance().compareTo(new BigDecimal("50000")));
  }

  @Test
  void registrationCreatesEnabledCustomerAndAcceptsOptionalPhone() throws Exception {
    mvc.perform(post("/register").with(csrf()).param("fullName", "New Customer")
        .param("email", " NEWUSER@Test.Local ").param("password", "TestPass123")
        .param("confirmPassword", "TestPass123").param("role", "ADMIN"))
        .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?registered"));
    User created = users.findByEmail("newuser@test.local").orElseThrow();
    assertEquals("USER", created.getRole());
    assertTrue(created.getEnabled());
    assertTrue(encoder.matches("TestPass123", created.getPassword()));
    mvc.perform(post("/login").with(csrf()).param("email", "newuser@test.local")
        .param("password", "TestPass123")).andExpect(org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated().withUsername("newuser@test.local"));
  }

  @Test
  void registrationRejectsShortPasswordAndDuplicateEmailClearly() throws Exception {
    mvc.perform(post("/register").with(csrf()).param("fullName", "New Customer")
        .param("email", "newuser@test.local").param("password", "1234567")
        .param("confirmPassword", "1234567"))
        .andExpect(status().isOk()).andExpect(model().attribute("error", "Mật khẩu phải có từ 8 đến 72 ký tự."));
    assertFalse(users.existsByEmail("newuser@test.local"));
    mvc.perform(post("/register").with(csrf()).param("fullName", "New Customer")
        .param("email", "CUSTOMER@test.local").param("password", "TestPass123")
        .param("confirmPassword", "TestPass123"))
        .andExpect(status().isOk()).andExpect(model().attribute("error", "Email này đã được sử dụng."));
  }
}
