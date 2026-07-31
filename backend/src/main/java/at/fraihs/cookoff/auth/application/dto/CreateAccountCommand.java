package at.fraihs.cookoff.auth.application.dto;

import at.fraihs.cookoff.auth.domain.model.SystemRole;

import java.util.Set;

/**
 * {@code password} is optional and only meaningful for ORGANIZER/ADMIN accounts — guests
 * never log in with a password (they use the access-link flow instead), per
 * docs/cookingChallenge/first-plan.md. Null/blank leaves {@code passwordHash} unset, meaning
 * that account can never pass {@code POST /api/v1/auth/login} until a password is set later.
 */
public record CreateAccountCommand(String email, String name, Set<SystemRole> initialRoles, String password) {
}
