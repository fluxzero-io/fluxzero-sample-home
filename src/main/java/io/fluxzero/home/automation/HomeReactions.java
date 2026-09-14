package io.fluxzero.home.automation;

import io.fluxzero.home.command.ChangeHomeMode;
import io.fluxzero.home.command.ReportDeviceStatus;
import io.fluxzero.home.model.Automation;
import io.fluxzero.home.model.DeviceObservationChanged;
import io.fluxzero.home.model.DeviceStatus;
import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeChange;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.home.model.HomeModeChanged;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.common.Message;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.tracking.Consumer;
import io.fluxzero.sdk.tracking.ThrowingErrorHandler;
import io.fluxzero.sdk.tracking.handling.HandleEvent;
import org.springframework.stereotype.Component;

import java.util.Map;

/** After-commit reactions; only source changes can trigger them, so scene execution cannot feed itself. */
@Component
@Consumer(name = "home-reactions", errorHandler = ThrowingErrorHandler.class)
public class HomeReactions {
    @HandleEvent
    void changed(ChangeHomeMode event, Graph<Home> home, Message message) {
        var previous = home.previous();
        react(home.get().id(), new HomeModeChanged(message.getTimestamp(),
                previous == null || previous.isEmpty() ? null : previous.get().mode(), home.get().mode()));
    }

    @HandleEvent
    void observed(ReportDeviceStatus event, Graph<DeviceStatus> status, Message message) {
        var home = status.ancestor(Home.class).orElseThrow();
        var previous = status.previous();
        react(home.get().id(), new DeviceObservationChanged(status.get().deviceId(), message.getTimestamp(),
                previous == null || previous.isEmpty() ? Map.of() : previous.get().readings(), status.get().readings()));
    }

    private void react(HomeId homeId, HomeChange change) {
        Fluxzero.search(Automation.class).whereParent(homeId).fetchAll().stream()
                .filter(automation -> automation.trigger().matches(change))
                .forEach(automation -> Fluxzero.sendCommandAndWait(new ReactToHome(automation.automationId(), change)));
    }
}
