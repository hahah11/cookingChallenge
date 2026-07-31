package at.fraihs.cookoff.auth.domain.model;

/**
 * System-level permission, unrelated to whether an Account is cooking or a
 * guest in any particular Challenge (see cookoff.domain.model.CookAssignment).
 * ADMIN: manage anything. ORGANIZER: create challenges, assign cooks/guests, score.
 * USER: score only.
 */
public enum SystemRole {
    ADMIN,
    ORGANIZER,
    USER
}
