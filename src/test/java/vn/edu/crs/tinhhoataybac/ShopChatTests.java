package vn.edu.crs.tinhhoataybac;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import vn.edu.crs.tinhhoataybac.model.User;
import vn.edu.crs.tinhhoataybac.repository.*;
import vn.edu.crs.tinhhoataybac.service.ShopChatService;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest @ActiveProfiles("test") @Transactional
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,TransactionalTestExecutionListener.class})
class ShopChatTests {
 @Autowired UserRepository users;@Autowired ChatMessageRepository messages;@Autowired ShopChatService chat;@Autowired WebApplicationContext context;
 User customer(String name,String role){var u=new User();u.setEmail(name+"@chat-test.invalid");u.setFullName("CHAT TEST "+name);u.setPassword("unused");u.setRole(role);return users.saveAndFlush(u);}
 org.springframework.test.web.servlet.MockMvc mvc(){return org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();}
 @Test void conversationReplyAndUnreadWork() throws Exception{var u=customer("buyer","USER");var a=customer("shop","ADMIN");chat.send(u.getId(),u,false,"Xin chào shop");assertEquals(1,messages.countByCustomerIdAndAdminSentAndReadByRecipientFalse(u.getId(),false));chat.read(u.getId(),true,messages.findAll().get(0).getId());assertEquals(0,messages.countByCustomerIdAndAdminSentAndReadByRecipientFalse(u.getId(),false));chat.send(u.getId(),a,true,"Shop chào bạn");assertEquals(1L,chat.thread(u.getId(),false).get("unread"));mvc().perform(get("/admin/chat").with(user(a.getEmail()).roles("ADMIN"))).andExpect(status().isOk());mvc().perform(get("/admin/api/chat").with(user(a.getEmail()).roles("ADMIN"))).andExpect(jsonPath("$[0].customerId").value(u.getId()));}
 @Test void customersCannotAccessAnotherConversationOrAdminInbox() throws Exception{var u=customer("one","USER");var other=customer("two","USER");chat.send(u.getId(),u,false,"PRIVATE CHAT");mvc().perform(get("/api/shop-chat").with(user(other.getEmail()))).andExpect(jsonPath("$.messages").isEmpty());mvc().perform(get("/admin/api/chat/"+u.getId()).with(user(other.getEmail()))).andExpect(status().isForbidden());assertThrows(IllegalStateException.class,()->chat.send(u.getId(),other,false,"WRONG"));}
 @Test void csrfAndRoleProtectionApply() throws Exception{var u=customer("csrf","USER");mvc().perform(post("/api/shop-chat/send").with(user(u.getEmail())).param("content","TEST")).andExpect(status().isForbidden());mvc().perform(post("/api/shop-chat/send").with(user(u.getEmail())).with(csrf()).param("content","TEST")).andExpect(status().isOk());mvc().perform(get("/api/shop-chat").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());mvc().perform(get("/api/shop-chat")).andExpect(status().is3xxRedirection());}
 @Test void emptyOversizedAndRapidMessagesAreRejected(){var u=customer("limit","USER");assertThrows(IllegalStateException.class,()->chat.send(u.getId(),u,false," "));assertThrows(IllegalStateException.class,()->chat.send(u.getId(),u,false,"x".repeat(1201)));for(int n=0;n<15;n++)chat.send(u.getId(),u,false,"TEST "+n);assertThrows(IllegalStateException.class,()->chat.send(u.getId(),u,false,"TOO FAST"));assertEquals(15,messages.count());}
}
