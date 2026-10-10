package vn.edu.crs.tinhhoataybac.service;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.math.BigDecimal;
import java.util.Map;
@Component
public class SePayIpnVerifier {
 private final String secret;
 public SePayIpnVerifier(@Value("${sepay.ipn-secret:${sepay.secret-key:}}") String secret){this.secret=secret;}
 public boolean authenticated(String supplied){return secret!=null&&!secret.isBlank()&&supplied!=null&&MessageDigest.isEqual(secret.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8));}
 public record Payment(String code, BigDecimal amount){}
 public Payment payment(Map<String,Object> payload){
  if(!"ORDER_PAID".equals(payload.get("notification_type"))) return null;
  if(!(payload.get("order") instanceof Map<?,?> o) || !(payload.get("transaction") instanceof Map<?,?> t)) throw new IllegalArgumentException("Missing payment data");
  if(!"CAPTURED".equals(o.get("order_status")) || !"APPROVED".equals(t.get("transaction_status")) || !"PAYMENT".equals(t.get("transaction_type")) || !"VND".equals(o.get("order_currency")) || !"VND".equals(t.get("transaction_currency")))throw new IllegalArgumentException("Invalid payment status");
  String code=String.valueOf(o.get("order_invoice_number")).trim().toUpperCase(java.util.Locale.ROOT);
  if(!code.matches("(?:THB|NAP)[1-9][0-9]{0,18}"))throw new IllegalArgumentException("Invalid invoice");
  BigDecimal amount=new BigDecimal(String.valueOf(o.get("order_amount"))), received=new BigDecimal(String.valueOf(t.get("transaction_amount")));
  if(amount.signum()<=0 || amount.compareTo(received)!=0 || amount.stripTrailingZeros().scale()>0 || amount.precision()>15)throw new IllegalArgumentException("Invalid amount");
  return new Payment(code,received);
 }
}
