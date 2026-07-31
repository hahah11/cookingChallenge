package at.fraihs.cookoff.auth.application.dto;

import at.fraihs.cookoff.auth.domain.model.SystemRole;

import java.util.Set;

public record CreateAccountCommand(String email, String name, Set<SystemRole> initialRoles) {
}
