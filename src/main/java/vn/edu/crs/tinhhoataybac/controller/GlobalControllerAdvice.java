package vn.edu.crs.tinhhoataybac.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.edu.crs.tinhhoataybac.model.User;
import vn.edu.crs.tinhhoataybac.service.CartService;
import vn.edu.crs.tinhhoataybac.service.UserService;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final CartService cartService;
    private final UserService userService;

    public GlobalControllerAdvice(
            CartService cartService,
            UserService userService) {

        this.cartService = cartService;
        this.userService = userService;
    }


    @ModelAttribute("cartCount")
    public int cartCount(
            HttpSession session) {

        return cartService
                .getTotalQuantity(session);
    }


    @ModelAttribute("loggedIn")
    public boolean loggedIn(
            Authentication authentication) {

        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication
                instanceof AnonymousAuthenticationToken);
    }


    @ModelAttribute("currentUser")
    public User currentUser(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication
                instanceof AnonymousAuthenticationToken) {

            return null;
        }

        return userService.findByEmail(
                authentication.getName()
        );
    }


    @ModelAttribute("isAdmin")
    public boolean isAdmin(
            Authentication authentication) {

        if (authentication == null) {
            return false;
        }

        return authentication
                .getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority()
                                .equals("ROLE_ADMIN")
                );
    }
}