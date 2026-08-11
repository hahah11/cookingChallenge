package at.fraihs.cookoff;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHash {
    static void main(String[] args) {
        System.out.println(
                new BCryptPasswordEncoder().encode("claude")
        );
    }
}
