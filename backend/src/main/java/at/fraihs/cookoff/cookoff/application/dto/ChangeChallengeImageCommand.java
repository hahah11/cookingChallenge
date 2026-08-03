package at.fraihs.cookoff.cookoff.application.dto;

public record ChangeChallengeImageCommand(String challengeId, String organizerAccountId, String contentType) {
}
