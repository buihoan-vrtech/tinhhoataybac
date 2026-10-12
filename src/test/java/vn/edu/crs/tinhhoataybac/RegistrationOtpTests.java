package vn.edu.crs.tinhhoataybac;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.*;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import vn.edu.crs.tinhhoataybac.service.*;
import vn.edu.crs.tinhhoataybac.repository.UserRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
@SpringBootTest @ActiveProfiles("test") @Transactional
@TestPropertySource(properties="app.mail.from=test-sender@example.invalid")
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,TransactionalTestExecutionListener.class})
class RegistrationOtpTests {
 @TestConfiguration static class MailConfig {@Bean @Primary RecordingMail mail(){return new RecordingMail();}}
 static class RecordingMail extends JavaMailSenderImpl {SimpleMailMessage last;boolean fail;@Override public void send(SimpleMailMessage message){if(fail)throw new org.springframework.mail.MailSendException("TEST FAILURE");last=message;}String code(){var m=java.util.regex.Pattern.compile("[0-9]{6}").matcher(last.getText());assertTrue(m.find());return m.group();}}
 @Autowired RecordingMail mail;@Autowired RegistrationVerificationService verification;@Autowired UserRepository users;@Autowired WebApplicationContext context;
 org.springframework.test.web.servlet.MockMvc mvc(){return org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();}
 @Test void registrationRequiresEmailCodeAndCreatesVerifiedUserOnce() throws Exception {
  var session=new MockHttpSession();var m=mvc();String email="otp-flow@test.invalid";
  m.perform(post("/register").session(session).with(csrf()).param("fullName","OTP TEST").param("email",email).param("password","Test123!").param("confirmPassword","Test123!").param("termsAccepted","true")).andExpect(redirectedUrl("/register/verify"));
  assertFalse(users.existsByEmail(email));String code=mail.code();assertEquals(email,mail.last.getTo()[0]);
  m.perform(get("/register/verify").session(session)).andExpect(status().isOk());
  m.perform(post("/register/verify").session(session).with(csrf()).param("code",code)).andExpect(redirectedUrl("/login?registered"));
  var u=users.findByEmail(email).orElseThrow();assertEquals("USER",u.getRole());assertTrue(u.getEnabled());assertNotNull(u.getEmailVerifiedAt());assertNotEquals("Test123!",u.getPassword());assertNotNull(verification.confirm(session,code));
 }
 @Test void wrongCodeAndResendAreLimited(){var session=new MockHttpSession();verification.start(session,"OTP TEST","otp-limit@test.invalid","Test123!","","");String code=mail.code();assertThrows(IllegalStateException.class,()->verification.resend(session));String wrong=code.equals("000000")?"111111":"000000";for(int n=0;n<5;n++)assertNotNull(verification.confirm(session,wrong));assertNotNull(verification.confirm(session,code));assertFalse(users.existsByEmail("otp-limit@test.invalid"));}
 @Test void mailFailureDoesNotCreateUserOrLeaveVerifiedSession(){var session=new MockHttpSession();mail.fail=true;try{assertThrows(IllegalStateException.class,()->verification.start(session,"OTP TEST","otp-fail@test.invalid","Test123!","",""));assertFalse(users.existsByEmail("otp-fail@test.invalid"));assertNull(verification.maskedEmail(session));}finally{mail.fail=false;}}
 @Test void codeCannotBeUsedFromAnotherSession(){var session=new MockHttpSession();verification.start(session,"OTP TEST","otp-session@test.invalid","Test123!","","");assertNotNull(verification.confirm(new MockHttpSession(),mail.code()));assertFalse(users.existsByEmail("otp-session@test.invalid"));}
}
