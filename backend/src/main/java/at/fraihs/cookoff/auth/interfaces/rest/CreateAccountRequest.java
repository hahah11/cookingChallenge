package at.fraihs.cookoff.auth.interfaces.rest;

import at.fraihs.cookoff.auth.domain.model.SystemRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record CreateAccountRequest(
        @NotBlank(message = "email is required") @Email(message = "must be a valid email address") String email,
        @NotBlank(message = "name is required") String name,
        Set<SystemRole> roles) {
}
