package vn.edu.crs.tinhhoataybac;
import org.junit.jupiter.api.Test;
import vn.edu.crs.tinhhoataybac.service.SePayIpnVerifier;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SePayIpnVerifierTests {
 private Map<String,Object> payload(){return new HashMap<>(Map.of("notification_type","ORDER_PAID","order",new HashMap<>(Map.of("order_status","CAPTURED","order_currency","VND","order_invoice_number","THB15","order_amount","50000.00")),"transaction",new HashMap<>(Map.of("transaction_status","APPROVED","transaction_type","PAYMENT","transaction_currency","VND","transaction_amount","50000"))));}
 @Test void rejectsAbsentOrWrongSecrets(){var v=new SePayIpnVerifier("test-secret");assertFalse(v.authenticated(null));assertFalse(v.authenticated("wrong"));assertTrue(v.authenticated("test-secret"));assertFalse(new SePayIpnVerifier("").authenticated(""));}
 @Test void acceptsCapturedApprovedVndPayment(){var payment=new SePayIpnVerifier("test").payment(payload());assertEquals("THB15",payment.code());assertEquals(0,payment.amount().compareTo(new java.math.BigDecimal("50000")));}
 @Test void ignoresOtherNotifications(){assertNull(new SePayIpnVerifier("test").payment(Map.of("notification_type","TRANSACTION_VOID")));}
 @Test void rejectsMissingDataAndWrongAmounts(){var v=new SePayIpnVerifier("test");assertThrows(IllegalArgumentException.class,()->v.payment(Map.of("notification_type","ORDER_PAID")));var p=payload();((Map<String,Object>)p.get("transaction")).put("transaction_amount","1");assertThrows(IllegalArgumentException.class,()->v.payment(p));}
 @Test void rejectsWrongCurrencyUnapprovedAndMalformedInvoice(){var v=new SePayIpnVerifier("test");for(String field:List.of("order_currency","order_status","order_invoice_number")){var p=payload();((Map<String,Object>)p.get("order")).put(field,"invalid");assertThrows(IllegalArgumentException.class,()->v.payment(p));}var p=payload();((Map<String,Object>)p.get("transaction")).put("transaction_status","DECLINED");assertThrows(IllegalArgumentException.class,()->v.payment(p));}
}
