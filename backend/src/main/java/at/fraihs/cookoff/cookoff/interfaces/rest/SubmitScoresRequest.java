package at.fraihs.cookoff.cookoff.interfaces.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitScoresRequest(@NotEmpty(message = "scores must not be empty") @Valid List<ScoreEntryRequest> scores) {
}
