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
            @RequestParam(defaultValue = "") String phone,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam(defaultValue = "") String address,
            @RequestParam(defaultValue = "false") boolean termsAccepted,
            Model model) {

        try {

            if (!termsAccepted) throw new IllegalStateException("Vui lòng đồng ý điều khoản sử dụng và chính sách bảo mật.");
            if (address.length() > 500) throw new IllegalStateException("Địa chỉ tối đa 500 ký tự.");
            if (!phone.isBlank() && !phone.replaceAll("[\\s().-]", "").matches("(?:0|\\+84)[0-9]{9,10}")) throw new IllegalStateException("Số điện thoại Việt Nam chưa hợp lệ.");
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
                    || password.length() < 8 || password.length() > 72) {

                throw new IllegalStateException(
                        "Mật khẩu phải có từ 8 đến 72 ký tự."
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
                    phone, address
            );

            return "redirect:/login?registered";

        } catch (Exception e) {
            String registrationError = e instanceof IllegalStateException
                ? e.getMessage() : "Không thể tạo tài khoản lúc này. Vui lòng thử lại.";
            if (e instanceof org.springframework.dao.DataIntegrityViolationException && userService.findByEmail(email) != null)
                registrationError = "Email này đã được sử dụng.";


            model.addAttribute(
                    "error",
                    registrationError
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

            model.addAttribute("address", address);
            return "register";
        }
    }
}