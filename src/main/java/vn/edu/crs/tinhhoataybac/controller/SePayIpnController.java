package vn.edu.crs.tinhhoataybac.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.crs.tinhhoataybac.service.*;
import java.util.Map;
@RestController
@RequestMapping("/api/sepay")
public class SePayIpnController {
 @org.springframework.beans.factory.annotation.Value("${sepay.gateway-order-payments-enabled:false}")
 private boolean gatewayOrderPaymentsEnabled;
 private final OrderService orders;
 private final WalletTopUpService wallets;
 private final SePayIpnVerifier verifier;
 public SePayIpnController(OrderService orders, WalletTopUpService wallets, SePayIpnVerifier verifier){this.orders=orders;this.wallets=wallets;this.verifier=verifier;}
 @PostMapping("/ipn")
 public ResponseEntity<?> receiveIpn(@RequestHeader(value="X-Secret-Key",required=false) String key,@RequestBody Map<String,Object> payload){
  if(!verifier.authenticated(key))return ResponseEntity.status(401).body(Map.of("success",false));
  try{
   var payment=verifier.payment(payload);
   if(payment==null)return ResponseEntity.ok(Map.of("success",true));
   if(payment.code().startsWith("THB")&&!gatewayOrderPaymentsEnabled)return ResponseEntity.ok(Map.of("success",true,"paymentApplied",false));
   boolean applied=payment.code().startsWith("THB")?orders.markOrderPaid(payment.code(),payment.amount()):wallets.confirmTopUp(payment.code(),payment.amount());
   return ResponseEntity.ok(Map.of("success",true,"paymentApplied",applied));
  }catch(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("success",false));}
  catch(Exception e){return ResponseEntity.internalServerError().body(Map.of("success",false));}
 }
}
