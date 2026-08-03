package at.fraihs.cookoff.cookoff.domain.model;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.event.ChallengeRevealed;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@AggregateRoot
public class Challenge {

    @Identity
    private final ChallengeId id;
    private final LocalDate date;
    private final String title;
    private final DishName dishName;
    private final List<CookAssignment> cookAssignments;
    private final List<AccountId> guestAccountIds;
    private ChallengeStatus status;
    private final AccountId createdBy;
    private String imageRef;

    private Challenge(ChallengeId id, LocalDate date, String title, DishName dishName,
                       List<CookAssignment> cookAssignments, List<AccountId> guestAccountIds,
                       ChallengeStatus status, AccountId createdBy, String imageRef) {
        this.id = id;
        this.date = date;
        this.title = title;
        this.dishName = dishName;
        this.cookAssignments = new ArrayList<>(cookAssignments);
        this.guestAccountIds = new ArrayList<>(guestAccountIds);
        this.status = status;
        this.createdBy = createdBy;
        this.imageRef = imageRef;
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
                List.copyOf(guestAccountIds), ChallengeStatus.OPEN, createdBy, null);
    }

    public static Challenge reconstitute(ChallengeId id, LocalDate date, String title, DishName dishName,
                                          List<CookAssignment> cookAssignments, List<AccountId> guestAccountIds,
                                          ChallengeStatus status, AccountId createdBy, String imageRef) {
        return new Challenge(id, date, title, dishName, cookAssignments, guestAccountIds, status, createdBy, imageRef);
    }

    /**
     * Reassigns either/both cooks and adds/removes guests in one atomic edit. Reassigning
     * either cook clears both cooks' picked plate colors, since a color pick is meaningless
     * once the person behind the label changes. A {@code null} cook id means "keep the
     * current cook for that label". Adding an already-present guest or removing an absent
     * one is a no-op, not an error.
     */
    public void editParticipants(AccountId newCookAAccountId, AccountId newCookBAccountId,
                                  List<AccountId> guestIdsToAdd, List<AccountId> guestIdsToRemove) {
        requireOpen();
        CookAssignment currentA = cookAssignmentFor(DishLabel.A);
        CookAssignment currentB = cookAssignmentFor(DishLabel.B);
        AccountId resolvedCookA = newCookAAccountId != null ? newCookAAccountId : currentA.accountId();
        AccountId resolvedCookB = newCookBAccountId != null ? newCookBAccountId : currentB.accountId();
        if (resolvedCookA.equals(resolvedCookB)) {
            throw new IllegalArgumentException("The two cooks must be different accounts");
        }

        boolean cooksChanged = !resolvedCookA.equals(currentA.accountId()) || !resolvedCookB.equals(currentB.accountId());
        if (cooksChanged) {
            cookAssignments.set(cookAssignments.indexOf(currentA), new CookAssignment(resolvedCookA, DishLabel.A));
            cookAssignments.set(cookAssignments.indexOf(currentB), new CookAssignment(resolvedCookB, DishLabel.B));
        }

        for (AccountId guestId : guestIdsToAdd) {
            if (!guestAccountIds.contains(guestId)) {
                guestAccountIds.add(guestId);
            }
        }
        guestAccountIds.removeAll(guestIdsToRemove);
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

    /**
     * A cook picks their plate color; the other cook is atomically assigned whichever color is
     * left. Irreversible once either cook has a color — first pick wins for the pair.
     */
    public void pickColor(AccountId cookAccountId, PlateColorId chosenColorId, PlateColorId otherColorId) {
        requireOpen();
        if (cookAssignments.stream().anyMatch(CookAssignment::hasColor)) {
            throw new IllegalStateException("Plate colors have already been picked for this challenge");
        }
        CookAssignment pickingAssignment = cookAssignments.stream()
                .filter(assignment -> assignment.accountId().equals(cookAccountId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account is not a cook of this challenge: " + cookAccountId));
        DishLabel otherLabel = pickingAssignment.label() == DishLabel.A ? DishLabel.B : DishLabel.A;
        CookAssignment otherAssignment = cookAssignmentFor(otherLabel);

        cookAssignments.set(cookAssignments.indexOf(pickingAssignment), pickingAssignment.withColor(chosenColorId));
        cookAssignments.set(cookAssignments.indexOf(otherAssignment), otherAssignment.withColor(otherColorId));
    }

    /** Replaces the challenge's photo reference; the old blob's lifecycle is the caller's concern. */
    public void changeImage(String newImageRef) {
        requireOpen();
        this.imageRef = newImageRef;
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

    public String getImageRef() {
        return imageRef;
    }
}
