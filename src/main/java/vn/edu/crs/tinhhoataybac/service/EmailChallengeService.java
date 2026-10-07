package vn.edu.crs.tinhhoataybac.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.crs.tinhhoataybac.model.*;
import vn.edu.crs.tinhhoataybac.repository.*;

@Service
public class EmailChallengeService {
  private final UserRepository users;
  private final EmailChallengeRepository challenges;
  private final PasswordEncoder encoder;
  private final ObjectProvider<JavaMailSender> mail;
  private final SecureRandom random = new SecureRandom();

  @Value("${app.mail.from:}")
  private String from;

  public EmailChallengeService(
      UserRepository users,
      EmailChallengeRepository challenges,
      PasswordEncoder encoder,
      ObjectProvider<JavaMailSender> mail) {
    this.users = users;
    this.challenges = challenges;
    this.encoder = encoder;
    this.mail = mail;
  }

  @Transactional
  public void send(String email, String purpose) {
    if (!java.util.Set.of("reset", "verify").contains(purpose))
      throw new IllegalStateException("Yêu cầu không hợp lệ.");
    User u = users.findByEmail(email.trim().toLowerCase(Locale.ROOT)).orElse(null);
    if (u == null) return;
    users.lockById(u.getId()).orElseThrow();
    if (mail.getIfAvailable() == null || from.isBlank())
      throw new IllegalStateException("Chức năng email cần cấu hình SMTP trên máy chủ.");
    var old = challenges.findFirstByUserIdAndPurposeOrderByIdDesc(u.getId(), purpose).orElse(null);
    if (old != null && old.getSentAt().isAfter(LocalDateTime.now().minusMinutes(1)))
      throw new IllegalStateException("Vui lòng đợi một phút trước khi gửi lại mã.");
    if (old != null) {
      old.setConsumed(true);
      challenges.save(old);
    }
    String code = String.format("%06d", random.nextInt(1000000));
    EmailChallenge e = new EmailChallenge();
    e.setUser(u);
    e.setPurpose(purpose);
    e.setCodeHash(encoder.encode(code));
    e.setSentAt(LocalDateTime.now());
    e.setExpiresAt(LocalDateTime.now().plusMinutes(10));
    challenges.save(e);
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(u.getEmail());
    message.setSubject("Tinh Hoa Tây Bắc · Mã xác nhận");
    message.setText(
        "Mã xác nhận của bạn là: "
            + code
            + ". Mã có hiệu lực trong 10 phút. Không chia sẻ mã này.");
    mail.getObject().send(message);
  }

  // Return validation failures so failed-attempt counters commit with the transaction.
  @Transactional
  public String confirm(String email, String purpose, String code, String password) {
    User u = users.findByEmail(email.trim().toLowerCase(Locale.ROOT)).orElse(null);
    if (u == null) return "Mã không hợp lệ hoặc đã hết hạn.";
    var latest =
        challenges.findFirstByUserIdAndPurposeOrderByIdDesc(u.getId(), purpose).orElse(null);
    if (latest == null) return "Mã không hợp lệ hoặc đã hết hạn.";
    EmailChallenge e = challenges.lockById(latest.getId()).orElseThrow();
    if (Boolean.TRUE.equals(e.getConsumed())
        || e.getAttempts() >= 5
        || !e.getExpiresAt().isAfter(LocalDateTime.now()))
      return "Mã đã hết hạn hoặc vượt quá số lần thử.";
    e.setAttempts(e.getAttempts() + 1);
    challenges.save(e);
    if (!encoder.matches(code, e.getCodeHash())) return "Mã xác nhận không đúng.";
    if ("reset".equals(purpose)) {
      if (password == null || password.length() < 8 || password.length() > 72)
        return "Mật khẩu cần từ 8 đến 72 ký tự.";
      u.setPassword(encoder.encode(password));
    } else u.setEmailVerifiedAt(LocalDateTime.now());
    e.setConsumed(true);
    challenges.save(e);
    users.save(u);
    return null;
  }
}
