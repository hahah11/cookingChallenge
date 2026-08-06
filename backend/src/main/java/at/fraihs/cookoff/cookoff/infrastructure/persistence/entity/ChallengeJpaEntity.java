package at.fraihs.cookoff.cookoff.infrastructure.persistence.entity;

import at.fraihs.cookoff.cookoff.domain.model.ChallengeStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "challenges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeJpaEntity {

    @Id
    private Long id;

    private String title;

    @Column(name = "challenge_date")
    private LocalDate challengeDate;

    @Column(name = "dish_name")
    private String dishName;

    @Column(name = "cook_a_account_id")
    private Long cookAAccountId;

    @Column(name = "cook_b_account_id")
    private Long cookBAccountId;

    @Column(name = "cook_a_color_id")
    private Long cookAColorId;

    @Column(name = "cook_b_color_id")
    private Long cookBColorId;

    @Enumerated(EnumType.STRING)
    private ChallengeStatus status;

    @Column(name = "created_by_account_id")
    private Long createdByAccountId;

    /** challenge_guests.id is an unmapped surrogate PK; Hibernate's @ElementCollection doesn't need it. */
    @ElementCollection
    @CollectionTable(name = "challenge_guests", joinColumns = @JoinColumn(name = "challenge_id"))
    @Column(name = "guest_account_id")
    private List<Long> guestAccountIds = new ArrayList<>();

    @Column(name = "image_ref")
    private String imageRef;

    /**
     * Separate from status because status flips back to OPEN on unreveal and can no longer
     * distinguish "never revealed" from "revealed, then unrevealed".
     */
    @Column(name = "has_been_revealed")
    private boolean hasBeenRevealed;

    @Column(name = "last_reveal_winner_account_id")
    private Long lastRevealWinnerAccountId;
}
