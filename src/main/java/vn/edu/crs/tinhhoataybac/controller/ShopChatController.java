package vn.edu.crs.tinhhoataybac.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import vn.edu.crs.tinhhoataybac.service.*;
import java.util.*;
@Controller public class ShopChatController {
 private final ShopChatService chat;private final UserService users;
 public ShopChatController(ShopChatService chat,UserService users){this.chat=chat;this.users=users;}
 @GetMapping("/admin/chat") public String page(){return "admin/chat";}
 @GetMapping("/api/shop-chat") @ResponseBody public Object own(Authentication a){return chat.thread(users.findByEmail(a.getName()).getId(),false);}
 @PostMapping("/api/shop-chat/send") @ResponseBody public ResponseEntity<?> send(@RequestParam String content,Authentication a){var u=users.findByEmail(a.getName());return attempt(()->chat.send(u.getId(),u,false,content));}
 @PostMapping("/api/shop-chat/read") @ResponseBody public ResponseEntity<?> read(@RequestParam Long through,Authentication a){return attempt(()->chat.read(users.findByEmail(a.getName()).getId(),false,through));}
 @GetMapping("/admin/api/chat") @ResponseBody public Object inbox(){return chat.inbox();}
 @GetMapping("/admin/api/chat/{id}") @ResponseBody public Object thread(@PathVariable Long id){return chat.thread(id,true);}
 @PostMapping("/admin/api/chat/{id}/send") @ResponseBody public ResponseEntity<?> reply(@PathVariable Long id,@RequestParam String content,Authentication a){return attempt(()->chat.send(id,users.findByEmail(a.getName()),true,content));}
 @PostMapping("/admin/api/chat/{id}/read") @ResponseBody public ResponseEntity<?> readAdmin(@PathVariable Long id,@RequestParam Long through){return attempt(()->chat.read(id,true,through));}
 private ResponseEntity<?> attempt(Runnable action){try{action.run();return ResponseEntity.ok(Map.of("success",true));}catch(IllegalStateException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}}
}
