package at.fraihs.cookoff.cookoff.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cook_rivalries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CookRivalryJpaEntity {

    @Id
    private Long id;

    @Column(name = "cook_a_account_id")
    private Long cookAAccountId;

    @Column(name = "cook_b_account_id")
    private Long cookBAccountId;

    @Column(name = "cook_a_wins")
    private int cookAWins;

    @Column(name = "cook_b_wins")
    private int cookBWins;

    private int draws;

    @Column(name = "total_challenges")
    private int totalChallenges;
}
