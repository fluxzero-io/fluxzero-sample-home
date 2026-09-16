package io.fluxzero.home.access;

import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.common.HasMessage;
import io.fluxzero.sdk.tracking.handling.authentication.AbstractUserProvider;
import io.fluxzero.sdk.tracking.handling.authentication.RefreshingUserProvider;
import io.fluxzero.sdk.tracking.handling.authentication.User;
import io.fluxzero.sdk.web.WebRequest;
import org.springframework.stereotype.Component;

@Component
public class HomeUserProvider extends AbstractUserProvider implements RefreshingUserProvider<HomeUser> {
    public HomeUserProvider() { super(HomeUser.class); }

    @Override public User fromMessage(HasMessage message) {
        User trusted = super.fromMessage(message);
        if (trusted != null) return trusted;
        if (!(message.toMessage() instanceof WebRequest)) return null;
        return BrowserSessions.find(message.getMetadata()).map(s -> getUserById(s.subject())).orElse(null);
    }

    @Override public HomeUser getUserById(Object id) {
        String subject = id.toString();
        if (subject.startsWith("$")) return null;
        return Fluxzero.loadModel(new AccountId(subject)).isEmpty() ? null : new HomeUser(subject);
    }

    @Override public HomeUser refreshUser(HomeUser user, HasMessage message) {
        return HomeUser.SYSTEM.equals(user) ? user : getUserById(user.id());
    }

    @Override public User getSystemUser() { return HomeUser.SYSTEM; }
}
