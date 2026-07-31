package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "score_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScoreSubmissionJpaEntity {

    @Id
    private Long id;

    @Column(name = "challenge_id")
    private Long challengeId;

    @Column(name = "guest_account_id")
    private Long guestAccountId;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @ElementCollection
    @CollectionTable(name = "scores", joinColumns = @JoinColumn(name = "submission_id"))
    private List<ScoreEmbeddable> scores = new ArrayList<>();
}
