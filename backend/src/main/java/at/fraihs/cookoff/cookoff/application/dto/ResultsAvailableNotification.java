package at.fraihs.cookoff.cookoff.application.dto;

import at.fraihs.cookoff.auth.domain.model.Email;

/**
 * Payload for
 * {@link at.fraihs.cookoff.cookoff.application.port.NotificationPort#sendResultsAvailable}, sent to
 * every guest and both cooks once an organizer reveals a cook-off. {@code link} carries a freshly
 * issued access link so a guest can open the results without already holding a session.
 */
public record ResultsAvailableNotification(Email recipient, String firstName, String challengeTitle, String link) {
}
