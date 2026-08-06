package at.fraihs.cookoff.auth.infrastructure.registrationinvite.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "registration_invites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationInviteJpaEntity {

    @Id
    private Long id;

    private Long issuedByAccountId;

    private Long challengeId;

    private String token;

    private Instant expiresAt;
}
