package vn.edu.crs.tinhhoataybac;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;
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
@TestPropertySource(properties={"sepay.ipn-secret=wallet-ipn-test-secret","sepay.webhook-secret=wallet-test-secret","payment.qr.account-no=0941943628","payment.qr.bank-id=MB"})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,TransactionalTestExecutionListener.class})
class WalletBankQrTests {
 @Autowired UserRepository users;@Autowired WalletTopUpService topUps;@Autowired WalletService wallets;@Autowired BankQrService qr;@Autowired WebApplicationContext context;
 User customer(String name){var u=new User();u.setEmail(name+"@wallet-test.invalid");u.setFullName("WALLET TEST");u.setRole("USER");u.setPassword("unused");return users.saveAndFlush(u);}
 org.springframework.test.web.servlet.MockMvc mvc(){return org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();}
 @Test void qrHasExactAmountAndMbAccountAndNeverUsesSandbox() throws Exception{var u=customer("qr");var t=topUps.createTopUp(u,new BigDecimal("50000"));String url=qr.topUpUrl(t);assertTrue(url.contains("acc=0941943628"));assertTrue(url.contains("bank=MB"));assertTrue(url.contains("amount=50000"));assertTrue(url.contains("des="+t.getPaymentCode()));mvc().perform(get("/wallet/topup/pay/"+t.getId()).with(user(u.getEmail()))).andExpect(status().isOk()).andExpect(view().name("wallet-topup-qr")).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("pay-sandbox"))));}
 @Test void authenticatedWebhookCreditsOnceAndStatusReflectsPayment() throws Exception{var u=customer("webhook");var t=topUps.createTopUp(u,new BigDecimal("50000"));String payload="{\"id\":456789,\"transferType\":\"in\",\"accountNumber\":\"0941943628\",\"content\":\"nap vi "+t.getPaymentCode()+"\",\"transferAmount\":50000}";var m=mvc();m.perform(post("/api/sepay/webhook").contentType("application/json").content(payload)).andExpect(status().isUnauthorized());assertEquals(0,wallets.getBalance(u).signum());for(int n=0;n<2;n++)m.perform(post("/api/sepay/webhook").header("Authorization","Apikey wallet-test-secret").contentType("application/json").content(payload)).andExpect(status().isOk());assertEquals(0,new BigDecimal("50000").compareTo(wallets.getBalance(u)));m.perform(get("/wallet/topup/status/"+t.getId()).with(user(u.getEmail()))).andExpect(jsonPath("$.paid").value(true));}
 @Test void wrongAmountsAndExpiredRequestCannotCredit(){var u=customer("invalid");var t=topUps.createTopUp(u,new BigDecimal("50000"));assertFalse(topUps.confirmTopUp(t.getPaymentCode(),new BigDecimal("49999")));assertFalse(topUps.confirmTopUp(t.getPaymentCode(),new BigDecimal("50001")));t.setCreatedAt(java.time.LocalDateTime.now().minusHours(1));assertFalse(topUps.confirmTopUp(t.getPaymentCode(),new BigDecimal("50000")));assertEquals(0,wallets.getBalance(u).signum());assertThrows(IllegalArgumentException.class,()->topUps.createTopUp(u,new BigDecimal("10000.5")));assertThrows(IllegalArgumentException.class,()->topUps.createTopUp(u,new BigDecimal("10000001")));}
 @Test void anotherCustomerCannotReadTopupAndSuccessUrlCannotCredit() throws Exception{var u=customer("owner");var other=customer("other");var t=topUps.createTopUp(u,new BigDecimal("50000"));var m=mvc();m.perform(get("/wallet/topup/status/"+t.getId()).with(user(other.getEmail()))).andExpect(status().isNotFound());m.perform(get("/wallet/topup/pay/"+t.getId()).with(user(other.getEmail()))).andExpect(status().is3xxRedirection());m.perform(get("/wallet/topup/success/"+t.getId()).with(user(u.getEmail()))).andExpect(status().is3xxRedirection());assertEquals(0,wallets.getBalance(u).signum());}
 @Test void sandboxGatewayCallbackCannotCreditBankQrRequest() throws Exception {
  var u=customer("sandbox-block");var t=topUps.createTopUp(u,new BigDecimal("50000"));
  String payload="{\"notification_type\":\"ORDER_PAID\",\"order\":{\"order_invoice_number\":\""+t.getPaymentCode()+"\",\"order_status\":\"CAPTURED\",\"order_currency\":\"VND\",\"order_amount\":50000},\"transaction\":{\"transaction_status\":\"APPROVED\",\"transaction_type\":\"PAYMENT\",\"transaction_currency\":\"VND\",\"transaction_amount\":50000}}";
  mvc().perform(post("/api/sepay/ipn").header("X-Secret-Key","wallet-ipn-test-secret").contentType("application/json").content(payload)).andExpect(status().isOk()).andExpect(jsonPath("$.paymentApplied").value(false));
  assertEquals("PENDING",t.getStatus());assertEquals(0,wallets.getBalance(u).signum());
 }
}
