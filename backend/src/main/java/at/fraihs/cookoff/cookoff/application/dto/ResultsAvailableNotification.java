package at.fraihs.cookoff.cookoff.application.dto;

import at.fraihs.cookoff.auth.domain.model.Email;

import java.util.Locale;

/**
 * Payload for
 * {@link at.fraihs.cookoff.cookoff.application.port.NotificationPort#sendResultsAvailable}, sent to
 * every guest and both cooks once an organizer reveals a cook-off. {@code link} carries a freshly
 * issued access link so a guest can open the results without already holding a session.
 *
 * <p>{@code canRate} and {@code picksPlateColor} pick the wording, exactly as on
 * {@link InvitationNotification}, as does {@code locale}.
 */
public record ResultsAvailableNotification(Email recipient, String firstName, String dishName, String link,
                                           boolean canRate, boolean picksPlateColor, Locale locale) {
}
