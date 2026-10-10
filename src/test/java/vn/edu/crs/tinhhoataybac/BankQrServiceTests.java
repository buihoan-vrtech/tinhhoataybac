package vn.edu.crs.tinhhoataybac;
import org.junit.jupiter.api.Test;
import vn.edu.crs.tinhhoataybac.model.Order;
import vn.edu.crs.tinhhoataybac.service.BankQrService;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
class BankQrServiceTests {
 @Test void qrUsesFullOrderTotalAndUniquePaymentCode(){Order o=new Order();o.setPaymentCode("THB11");o.setSubtotal(new BigDecimal("150000"));o.setShippingFee(new BigDecimal("25000"));o.setTotalAmount(new BigDecimal("175000.00"));String url=new BankQrService("MB","0000000000","TEST").url(o);assertTrue(url.contains("amount=175000"));assertTrue(url.contains("des=THB11"));assertTrue(url.contains("acc=0000000000"));assertFalse(url.contains("amount=150000"));}
 @Test void rejectsInvalidAmountsAndMissingBank(){Order o=new Order();o.setPaymentCode("THB1");o.setTotalAmount(BigDecimal.ZERO);assertThrows(IllegalStateException.class,()->new BankQrService("MB","0000000000","TEST").url(o));o.setTotalAmount(new BigDecimal("1.5"));assertThrows(IllegalStateException.class,()->new BankQrService("MB","0000000000","TEST").url(o));o.setTotalAmount(new BigDecimal("10000"));assertThrows(IllegalStateException.class,()->new BankQrService("","","TEST").url(o));}
}
