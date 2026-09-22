package at.fraihs.cookoff.auth.infrastructure.passwordreset.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetTokenJpaEntity {

    @Id
    private Long id;

    private Long accountId;

    private String token;

    private Instant expiresAt;

    private Instant usedAt;

    private Instant createdAt;
}
