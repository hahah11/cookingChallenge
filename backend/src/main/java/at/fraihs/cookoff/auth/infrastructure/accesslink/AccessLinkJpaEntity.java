package at.fraihs.cookoff.auth.infrastructure.accesslink;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "access_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccessLinkJpaEntity {

    @Id
    private Long id;

    private Long accountId;

    private Long challengeId;

    private String token;

    private Instant expiresAt;

    private Instant usedAt;

    private Instant createdAt;
}
