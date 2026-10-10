package vn.edu.crs.tinhhoataybac.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;


@Configuration
public class SecurityConfig {


    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }


    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http)
            throws Exception {


        /*
         * ==========================================
         * CSRF
         * ==========================================
         *
         * SePay gọi từ server bên ngoài nên không có
         * CSRF token của website.
         *
         * Chỉ bỏ CSRF đối với các endpoint SePay.
         */
        http.csrf(csrf -> csrf

                .ignoringRequestMatchers(
                        "/api/sepay/ipn",
                        "/api/sepay/webhook"
                )
        );


        /*
         * ==========================================
         * PHÂN QUYỀN URL
         * ==========================================
         */
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/products/*/reviews").authenticated()


                /*
                 * FILE TĨNH
                 */
                .requestMatchers(
                        "/css/**",
                        "/js/**",
                        "/images/**", "/uploads/**"
                )
                .permitAll()


                /*
                 * PUBLIC
                 *
                 * Không cần đăng nhập.
                 */
                .requestMatchers(
                        "/",
                        "/products/**",
                        "/login",
                        "/register",
                        "/forgot-password", "/reset-password",

                        "/api/sepay/ipn",
                        "/api/sepay/webhook",

                        "/products-search-suggestions", "/khuyen-mai"
                )
                .permitAll()

                .requestMatchers(org.springframework.http.HttpMethod.GET,"/combos").permitAll()
                .requestMatchers("/combos/**").authenticated()


                /*
                 * ADMIN
                 */
                .requestMatchers(
                        "/admin/**"
                )
                .hasRole("ADMIN")


                /*
                 * PHẢI ĐĂNG NHẬP
                 *
                 * Bao gồm cả giỏ hàng.
                 *
                 * Vì vậy:
                 * chưa login -> không thêm được giỏ hàng.
                 */
                .requestMatchers(
                        "/cart/**",
                        "/checkout/**",
                        "/payment/**",
                        "/order-success/**",
                        "/wallet/**",
                        "/account/**", "/api/payment/status/**"
                )
                .authenticated()


                /*
                 * Các URL còn lại
                 */
                .anyRequest()
                .permitAll()
        );


        /*
         * ==========================================
         * LOGIN
         * ==========================================
         */
        http.formLogin(form -> form

                .loginPage("/login")

                .loginProcessingUrl("/login")

                /*
                 * Username chính là email
                 */
                .usernameParameter("email")

                .passwordParameter("password")

                .permitAll()
        );


        /*
         * ==========================================
         * LOGOUT
         * ==========================================
         */
        http.logout(logout -> logout

                .logoutUrl("/logout")

                .logoutSuccessUrl("/")

                .permitAll()
        );


        return http.build();
    }
}
