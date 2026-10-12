package vn.edu.crs.tinhhoataybac.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import vn.edu.crs.tinhhoataybac.model.Order;
@Service
public class BankQrService {
 private final String bank,account,holder;
 public BankQrService(@Value("${payment.qr.bank-id:}") String bank,@Value("${payment.qr.account-no:}") String account,@Value("${payment.qr.account-name:}") String holder){this.bank=bank;this.account=account;this.holder=holder;}
 public String account(){return account;} public String bank(){return bank;} public String holder(){return holder;}
 public String url(Order o){return paymentUrl(o.getTotalAmount(),o.getPaymentCode());}
 public String topUpUrl(vn.edu.crs.tinhhoataybac.model.WalletTopUp t){return paymentUrl(t.getAmount(),t.getPaymentCode());}
 private String paymentUrl(java.math.BigDecimal amount,String code){
  if(bank.isBlank()||!account.matches("[A-Za-z0-9]{1,19}")||holder.isBlank())throw new IllegalStateException("Chưa cấu hình tài khoản nhận thanh toán.");
  if(amount==null||amount.signum()<=0||amount.stripTrailingZeros().scale()>0||code==null||!code.matches("(?:THB|NAP)[1-9][0-9]*"))throw new IllegalStateException("Thông tin thanh toán chưa hợp lệ.");
  return UriComponentsBuilder.fromUriString("https://vietqr.app/img").queryParam("bank",bank).queryParam("acc",account).queryParam("amount",amount.toBigIntegerExact().toString()).queryParam("des",code).queryParam("template","qronly").build().encode().toUriString();
 }
}
