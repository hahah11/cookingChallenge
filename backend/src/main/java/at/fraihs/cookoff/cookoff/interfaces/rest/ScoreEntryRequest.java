package at.fraihs.cookoff.cookoff.interfaces.rest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Wire shape matches first-plan.md's { dish: "A"|"B", category, points } exactly. */
public record ScoreEntryRequest(
        @NotBlank(message = "dish is required") String dish,
        @NotBlank(message = "category is required") String category,
        @NotNull(message = "points is required") @Min(0) @Max(5) Integer points) {
}
