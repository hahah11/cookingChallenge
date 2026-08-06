package at.fraihs.cookoff.cookoff.infrastructure.image.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "challenge_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeImageJpaEntity {

    @Id
    private Long id;

    @Column(name = "content_type")
    private String contentType;

    @Lob
    private byte[] data;

    @Column(name = "created_at")
    private Instant createdAt;
}
