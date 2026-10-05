package vn.edu.crs.tinhhoataybac.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.crs.tinhhoataybac.model.User;
import vn.edu.crs.tinhhoataybac.repository.UserRepository;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    public User register(String fullName,
                         String email,
                         String password,
                         String phone) {

        email = email.trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {

            throw new IllegalStateException(
                    "Email này đã được sử dụng."
            );
        }

        User user = new User();

        user.setFullName(fullName.trim());
        user.setEmail(email);

        user.setPassword(
                passwordEncoder.encode(password)
        );

        user.setPhone(
                phone != null
                        ? phone.trim()
                        : null
        );

        user.setRole("USER");
        user.setEnabled(true);

        return userRepository.save(user);
    }


    public User findByEmail(String email) {

        if (email == null) {
            return null;
        }

        return userRepository
                .findByEmail(
                        email.trim().toLowerCase()
                )
                .orElse(null);
    }


    @Override
    public UserDetails loadUserByUsername(
            String email)
            throws UsernameNotFoundException {

        User user = userRepository
                .findByEmail(
                        email.trim().toLowerCase()
                )
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Không tìm thấy tài khoản."
                        )
                );

        return org.springframework.security
                .core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole())
                .disabled(!Boolean.TRUE.equals(
                        user.getEnabled()
                ))
                .build();
    }
}