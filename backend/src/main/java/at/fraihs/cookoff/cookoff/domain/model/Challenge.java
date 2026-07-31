package at.fraihs.cookoff.cookoff.domain.model;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.event.ChallengeRevealed;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Challenge {

    private final ChallengeId id;
    private final LocalDate date;
    private final String title;
    private final DishName dishName;
    private final List<CookAssignment> cookAssignments;
    private final List<AccountId> guestAccountIds;
    private ChallengeStatus status;
    private final AccountId createdBy;

    private Challenge(ChallengeId id, LocalDate date, String title, DishName dishName,
                       List<CookAssignment> cookAssignments, List<AccountId> guestAccountIds,
                       ChallengeStatus status, AccountId createdBy) {
        this.id = id;
        this.date = date;
        this.title = title;
        this.dishName = dishName;
        this.cookAssignments = new ArrayList<>(cookAssignments);
        this.guestAccountIds = new ArrayList<>(guestAccountIds);
        this.status = status;
        this.createdBy = createdBy;
    }

    public static Challenge create(LocalDate date, String title, DishName dishName,
                                    AccountId cookAAccountId, AccountId cookBAccountId,
                                    List<AccountId> guestAccountIds, AccountId createdBy) {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        if (dishName == null) {
            throw new IllegalArgumentException("dishName must not be null");
        }
        if (cookAAccountId == null || cookBAccountId == null) {
            throw new IllegalArgumentException("Both cooks must be assigned");
        }
        if (cookAAccountId.equals(cookBAccountId)) {
            throw new IllegalArgumentException("The two cooks must be different accounts");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("createdBy must not be null");
        }
        List<CookAssignment> assignments = List.of(
                new CookAssignment(cookAAccountId, DishLabel.A),
                new CookAssignment(cookBAccountId, DishLabel.B)
        );
        return new Challenge(ChallengeId.generate(), date, title, dishName, assignments,
                List.copyOf(guestAccountIds), ChallengeStatus.OPEN, createdBy);
    }

    public static Challenge reconstitute(ChallengeId id, LocalDate date, String title, DishName dishName,
                                          List<CookAssignment> cookAssignments, List<AccountId> guestAccountIds,
                                          ChallengeStatus status, AccountId createdBy) {
        return new Challenge(id, date, title, dishName, cookAssignments, guestAccountIds, status, createdBy);
    }

    public void addGuest(AccountId guestAccountId) {
        requireOpen();
        if (guestAccountIds.contains(guestAccountId)) {
            throw new IllegalStateException("Account is already a guest of this challenge");
        }
        guestAccountIds.add(guestAccountId);
    }

    /**
     * Transitions to REVEALED and returns the event to publish. The overall winner is
     * computed beforehand by ResultCalculator (it needs the ScoreSubmissions, which live
     * outside this aggregate) — the application layer orchestrates that, then calls this.
     */
    public ChallengeRevealed reveal(AccountId overallWinnerAccountId) {
        requireOpen();
        this.status = ChallengeStatus.REVEALED;
        AccountId cookA = cookAssignmentFor(DishLabel.A).accountId();
        AccountId cookB = cookAssignmentFor(DishLabel.B).accountId();
        return new ChallengeRevealed(id, cookA, cookB, overallWinnerAccountId);
    }

    public CookAssignment cookAssignmentFor(DishLabel label) {
        return cookAssignments.stream()
                .filter(assignment -> assignment.label() == label)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No cook assigned to label " + label));
    }

    public boolean isGuest(AccountId accountId) {
        return guestAccountIds.contains(accountId);
    }

    /** Either of the two cooks, or a pre-added guest — the full set of people who may score. */
    public boolean isParticipant(AccountId accountId) {
        return isGuest(accountId)
                || cookAssignments.stream().anyMatch(assignment -> assignment.accountId().equals(accountId));
    }

    private void requireOpen() {
        if (status != ChallengeStatus.OPEN) {
            throw new IllegalStateException("Challenge is not open (status=" + status + ")");
        }
    }

    public ChallengeId getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getTitle() {
        return title;
    }

    public DishName getDishName() {
        return dishName;
    }

    public List<CookAssignment> getCookAssignments() {
        return List.copyOf(cookAssignments);
    }

    public List<AccountId> getGuestAccountIds() {
        return List.copyOf(guestAccountIds);
    }

    public ChallengeStatus getStatus() {
        return status;
    }

    public AccountId getCreatedBy() {
        return createdBy;
    }
}
