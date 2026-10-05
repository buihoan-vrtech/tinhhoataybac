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

        http

                .authorizeHttpRequests(auth -> auth

                        // STATIC
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        )
                        .permitAll()


                        // PUBLIC
                        .requestMatchers(
                                "/",
                                "/products/**",
                                "/cart/**",
                                "/login",
                                "/register"
                        )
                        .permitAll()


                        // ADMIN
                        .requestMatchers(
                                "/admin/**"
                        )
                        .hasRole("ADMIN")


                        // USER PHẢI ĐĂNG NHẬP
                        .requestMatchers(
                                "/checkout/**",
                                "/payment/**",
                                "/order-success/**",
                                "/wallet/**",
                                "/account/**"
                        )
                        .authenticated()


                        .anyRequest()
                        .permitAll()
                )


                .formLogin(form -> form

                        .loginPage("/login")

                        .loginProcessingUrl("/login")

                        .usernameParameter("email")

                        .passwordParameter("password")

                        .permitAll()
                )


                .logout(logout -> logout

                        .logoutUrl("/logout")

                        .logoutSuccessUrl("/")

                        .permitAll()
                );

        return http.build();
    }
}