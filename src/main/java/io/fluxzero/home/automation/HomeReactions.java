package io.fluxzero.home.automation;

import io.fluxzero.home.command.ChangeHomeMode;
import io.fluxzero.home.command.ReportDeviceStatus;
import io.fluxzero.home.model.Automation;
import io.fluxzero.home.model.AutomationTrigger;
import io.fluxzero.home.model.DeviceStatus;
import io.fluxzero.home.model.DeviceStatusId;
import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.common.Message;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.tracking.Consumer;
import io.fluxzero.sdk.tracking.ThrowingErrorHandler;
import io.fluxzero.sdk.tracking.handling.HandleEvent;
import java.util.Map;
import org.springframework.stereotype.Component;

/** After-commit reactions; only source changes can trigger them, so scene execution cannot feed itself. */
@Component
@Consumer(name = "home-reactions", errorHandler = ThrowingErrorHandler.class)
public class HomeReactions {
    @HandleEvent
    void changed(ChangeHomeMode event, Graph<Home> home, Message message) {
        var previous = home.previous();
        react(home.get().homeId(), new HomeSignal(null, home.revisionStateIndex(), message.getTimestamp(),
                previous == null || previous.get() == null ? null : previous.get().mode(), home.get().mode(), Map.of(), Map.of()));
    }
    @HandleEvent
    void observed(ReportDeviceStatus event, Graph<DeviceStatus> status, Message message) {
        var home = status.ancestor(Home.class).orElseThrow();
        var previous = status.previous();
        react(home.get().homeId(), new HomeSignal(status.get().deviceStatusId(), status.revisionStateIndex(), message.getTimestamp(),
                null, null, previous == null || previous.isEmpty() ? Map.of() : previous.get().readings(), status.get().readings()));
    }
    private void react(HomeId homeId, HomeSignal signal) {
        Fluxzero.search(Automation.class).whereParent(homeId).fetchAll().forEach(automation -> {
            boolean sameSource = signal.statusId() == null
                    ? automation.trigger() instanceof AutomationTrigger.HomeBecomes
                    : automation.trigger() instanceof AutomationTrigger.MeasurementCrosses t
                        && signal.statusId().equals(new DeviceStatusId(t.deviceId().getFunctionalId()));
            if (sameSource) Fluxzero.sendCommandAndWait(new ReactToHome(automation.automationId(), signal));
        });
    }
}
