package vn.edu.crs.tinhhoataybac.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.crs.tinhhoataybac.service.UserService;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(
            UserService userService) {

        this.userService = userService;
    }


    @GetMapping("/login")
    public String login() {

        return "login";
    }


    @GetMapping("/register")
    public String register() {

        return "register";
    }


    @PostMapping("/register")
    public String registerUser(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            Model model) {

        try {

            if (fullName == null
                    || fullName.trim().isEmpty()) {

                throw new IllegalStateException(
                        "Vui lòng nhập họ tên."
                );
            }

            if (email == null
                    || email.trim().isEmpty()) {

                throw new IllegalStateException(
                        "Vui lòng nhập email."
                );
            }

            if (password == null
                    || password.length() < 6) {

                throw new IllegalStateException(
                        "Mật khẩu phải có ít nhất 6 ký tự."
                );
            }

            if (!password.equals(confirmPassword)) {

                throw new IllegalStateException(
                        "Mật khẩu nhập lại không khớp."
                );
            }

            userService.register(
                    fullName,
                    email,
                    password,
                    phone
            );

            return "redirect:/login?registered";

        } catch (Exception e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            model.addAttribute(
                    "fullName",
                    fullName
            );

            model.addAttribute(
                    "email",
                    email
            );

            model.addAttribute(
                    "phone",
                    phone
            );

            return "register";
        }
    }
}