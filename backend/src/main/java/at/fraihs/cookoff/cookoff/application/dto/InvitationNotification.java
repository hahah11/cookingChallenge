package at.fraihs.cookoff.cookoff.application.dto;

import at.fraihs.cookoff.auth.domain.model.Email;

import java.util.Locale;

/**
 * Payload for {@link at.fraihs.cookoff.cookoff.application.port.NotificationPort#sendAccessLink}:
 * everything the invitation mail needs to address the recipient and name the cook-off they are
 * being invited to. {@code link} is the full {@code .../home?token=...} URL — guests have no
 * password, so it is their only way in.
 *
 * <p>{@code canRate} and {@code picksPlateColor} pick the wording: guests rate, cooks choose a
 * plate color. They are independent flags, not one role — a cook can also be a guest on the same
 * cook-off, and then both apply.
 *
 * <p>{@code locale} is the recipient's preferred language.
 */
public record InvitationNotification(Email recipient, String firstName, String dishName, String link,
                                     boolean canRate, boolean picksPlateColor, Locale locale) {
}
