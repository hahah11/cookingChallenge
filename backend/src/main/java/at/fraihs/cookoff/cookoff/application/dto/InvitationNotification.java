package at.fraihs.cookoff.cookoff.application.dto;

import at.fraihs.cookoff.auth.domain.model.Email;

/**
 * Payload for {@link at.fraihs.cookoff.cookoff.application.port.NotificationPort#sendAccessLink}:
 * everything the invitation mail needs to address the recipient and name the cook-off they are
 * being invited to. {@code link} is the full {@code .../home?token=...} URL — guests have no
 * password, so it is their only way in.
 */
public record InvitationNotification(Email recipient, String firstName, String challengeTitle, String link) {
}
