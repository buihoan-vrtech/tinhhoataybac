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
 public String url(Order o){
  if(bank.isBlank()||!account.matches("[A-Za-z0-9]{1,19}")||holder.isBlank())throw new IllegalStateException("Chưa cấu hình tài khoản nhận thanh toán.");
  if(o.getTotalAmount()==null||o.getTotalAmount().signum()<=0||o.getTotalAmount().stripTrailingZeros().scale()>0||o.getPaymentCode()==null||!o.getPaymentCode().matches("THB[1-9][0-9]*"))throw new IllegalStateException("Thông tin thanh toán chưa hợp lệ.");
  return UriComponentsBuilder.fromUriString("https://vietqr.app/img").queryParam("bank",bank).queryParam("acc",account).queryParam("amount",o.getTotalAmount().toBigIntegerExact().toString()).queryParam("des",o.getPaymentCode()).queryParam("template","qronly").build().encode().toUriString();
 }
}
