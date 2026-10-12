package vn.edu.crs.tinhhoataybac.service;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.time.Instant;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.crs.tinhhoataybac.model.User;

@Service
public class RegistrationVerificationService {
 private static final String KEY="registrationVerification";
 private final UserService users; private final PasswordEncoder encoder;
 private final ObjectProvider<JavaMailSender> mail; private final SecureRandom random=new SecureRandom();
 private final ConcurrentHashMap<String,Instant> sent=new ConcurrentHashMap<>();
 @Value("${app.mail.from:}") private String from;
 public RegistrationVerificationService(UserService users,PasswordEncoder encoder,ObjectProvider<JavaMailSender> mail){this.users=users;this.encoder=encoder;this.mail=mail;}
 private record Pending(String name,String email,String passwordHash,String phone,String address,String codeHash,Instant expires,int attempts,Instant firstSent,int sends) implements Serializable {}
 public void start(HttpSession session,String name,String email,String password,String phone,String address){
  User draft=users.prepareRegistration(name,email,password,phone,address);
  synchronized(session){Pending old=(Pending)session.getAttribute(KEY);Instant now=Instant.now();int sends=old!=null&&old.firstSent().isAfter(now.minusSeconds(600))?old.sends():0;
   if(sends>=5)throw new IllegalStateException("Bạn đã gửi nhiều mã. Vui lòng thử lại sau 10 phút.");
   Pending p=new Pending(draft.getFullName(),draft.getEmail(),draft.getPassword(),draft.getPhone(),draft.getAddress(),null,now,0,sends==0?now:old.firstSent(),sends);
   send(session,p);
  }
 }
 public void resend(HttpSession session){synchronized(session){Pending p=pending(session);if(p==null)throw new IllegalStateException("Vui lòng nhập lại thông tin đăng ký.");if(p.sends()>=5&&p.firstSent().isAfter(Instant.now().minusSeconds(600)))throw new IllegalStateException("Bạn đã gửi nhiều mã. Vui lòng thử lại sau 10 phút.");send(session,p);}}
 private void send(HttpSession session,Pending p){
  JavaMailSender sender=mail.getIfAvailable();if(sender==null||from==null||from.isBlank())throw new IllegalStateException("Chưa thể gửi mã xác minh. Vui lòng liên hệ shop hoặc thử lại sau.");
  Instant now=Instant.now();synchronized(sent){sent.entrySet().removeIf(e->e.getValue().isBefore(now.minusSeconds(600)));if(sent.size()>10000)throw new IllegalStateException("Hệ thống đang bận. Vui lòng thử lại sau.");Instant last=sent.get(p.email());if(last!=null&&last.isAfter(now.minusSeconds(60)))throw new IllegalStateException("Vui lòng đợi 60 giây trước khi gửi lại mã.");sent.put(p.email(),now);}
  String code=String.format("%06d",random.nextInt(1000000));
  SimpleMailMessage message=new SimpleMailMessage();message.setFrom(from);message.setTo(p.email());message.setSubject("Tinh Hoa Tây Bắc · Xác minh đăng ký");message.setText("Mã xác minh đăng ký của bạn: "+code+"\nMã có hiệu lực 10 phút. Không chia sẻ mã cho bất kỳ ai. Nếu bạn không đăng ký, hãy bỏ qua thư này.");
  try{sender.send(message);}catch(org.springframework.mail.MailException e){throw new IllegalStateException("Không gửi được email. Vui lòng thử lại sau hoặc liên hệ shop.");}
  int sends=p.firstSent().isBefore(now.minusSeconds(600))?1:p.sends()+1;
  session.setAttribute(KEY,new Pending(p.name(),p.email(),p.passwordHash(),p.phone(),p.address(),encoder.encode(code),now.plusSeconds(600),0,sends==1?now:p.firstSent(),sends));
 }
 private Pending pending(HttpSession session){return (Pending)session.getAttribute(KEY);}
 public String maskedEmail(HttpSession session){Pending p=pending(session);if(p==null)return null;int at=p.email().indexOf('@');return p.email().substring(0,Math.min(2,at))+"***"+p.email().substring(at);}
 public String confirm(HttpSession session,String code){synchronized(session){Pending p=pending(session);if(p==null)return "Phiên xác minh đã hết. Vui lòng đăng ký lại.";if(p.attempts()>=5)return "Đã vượt quá 5 lần nhập sai. Vui lòng gửi mã mới.";if(!p.expires().isAfter(Instant.now()))return "Mã đã hết hạn. Vui lòng gửi mã mới.";
  if(code==null||!code.matches("[0-9]{6}")||!encoder.matches(code,p.codeHash())){session.setAttribute(KEY,new Pending(p.name(),p.email(),p.passwordHash(),p.phone(),p.address(),p.codeHash(),p.expires(),p.attempts()+1,p.firstSent(),p.sends()));return "Mã xác minh chưa đúng. Vui lòng kiểm tra lại email.";}
  User u=new User();u.setFullName(p.name());u.setEmail(p.email());u.setPassword(p.passwordHash());u.setPhone(p.phone());u.setAddress(p.address());users.saveVerifiedRegistration(u);session.removeAttribute(KEY);return null;
 }}
}
