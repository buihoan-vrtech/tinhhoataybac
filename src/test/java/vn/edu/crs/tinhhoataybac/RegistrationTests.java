package vn.edu.crs.tinhhoataybac;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.crs.tinhhoataybac.controller.AuthController;
import vn.edu.crs.tinhhoataybac.service.UserService;
import vn.edu.crs.tinhhoataybac.model.User;
import vn.edu.crs.tinhhoataybac.repository.UserRepository;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
class RegistrationTests {
 private final AtomicReference<User> saved=new AtomicReference<>();
 private UserService service(){ UserRepository repo=(UserRepository)Proxy.newProxyInstance(UserRepository.class.getClassLoader(),new Class[]{UserRepository.class},(p,m,a)->switch(m.getName()){case "existsByEmail"->false;case "save"->{saved.set((User)a[0]);yield a[0];}default->throw new UnsupportedOperationException(m.getName());});return new UserService(repo,new PasswordEncoder(){public String encode(CharSequence s){return "encoded";}public boolean matches(CharSequence s,String e){return false;}});}
 @Test void savesOptionalAddressAndNormalizesEmail(){var c=new AuthController(service());assertEquals("redirect:/login?registered",c.registerUser(" Test ","TEST@example.com","0912345678","Test123!","Test123!"," Hanoi ",true,new ExtendedModelMap()));assertEquals("Hanoi",saved.get().getAddress());assertEquals("test@example.com",saved.get().getEmail());assertEquals("encoded",saved.get().getPassword());}
 @Test void rejectsMissingConsentWithoutSaving(){var model=new ExtendedModelMap();assertEquals("register",new AuthController(service()).registerUser("Test","test@example.com","","Test123!","Test123!","",false,model));assertNull(saved.get());assertTrue(model.get("error").toString().contains("đồng ý"));}
 @Test void rejectsMismatchInvalidPhoneAndOversizedAddress(){var c=new AuthController(service());for(int i=0;i<3;i++){var m=new ExtendedModelMap();assertEquals("register",c.registerUser("Test","test@example.com",i==0?"abc":"0912345678","Test123!",i==1?"different":"Test123!",i==2?"x".repeat(501):"",true,m));assertNotNull(m.get("error"));assertNull(saved.get());}}
}
