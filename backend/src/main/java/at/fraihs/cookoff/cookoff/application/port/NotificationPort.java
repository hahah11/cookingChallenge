package at.fraihs.cookoff.cookoff.application.port;

import at.fraihs.cookoff.auth.domain.model.Email;

public interface NotificationPort {

    void sendAccessLink(Email email, String link);
}
