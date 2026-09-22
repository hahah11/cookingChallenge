package at.fraihs.cookoff.cookoff.application.port;

import at.fraihs.cookoff.cookoff.application.dto.InvitationNotification;
import at.fraihs.cookoff.cookoff.application.dto.ResultsAvailableNotification;

/**
 * Outgoing port for participant-facing notifications. Implementations must never let a delivery
 * failure escape: a mail server being down is not a reason to fail the organizer's request.
 */
public interface NotificationPort {

    /** "Send links" — invites a guest or cook to score a cook-off via a personal access link. */
    void sendAccessLink(InvitationNotification notification);

    /** Tells a participant that a cook-off they took part in has been revealed. */
    void sendResultsAvailable(ResultsAvailableNotification notification);
}
