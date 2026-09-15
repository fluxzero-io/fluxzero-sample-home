package io.fluxzero.home.homeassistant;

import io.fluxzero.home.command.DeviceCommand;
import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceStatus;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.scheduling.ScheduleId;
import io.fluxzero.sdk.tracking.Consumer;
import io.fluxzero.sdk.tracking.ThrowingErrorHandler;
import io.fluxzero.sdk.tracking.handling.HandleEvent;
import io.fluxzero.sdk.tracking.handling.HandleSchedule;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Concrete HA orchestration. Physical effects run after commit on one event consumer; domain applies remain pure. */
@Component
@Consumer(name = "home-assistant", singleTracker = true, errorHandler = ThrowingErrorHandler.class)
public class HomeAssistantIntegration {
    public static ScheduleId refreshSchedule(HomeAssistantId id) { return ScheduleId.of("home-assistant-refresh", id); }
    public static ScheduleId deliverySchedule(DeviceId id) { return ScheduleId.of("home-assistant-delivery", id); }

    private HomeAssistantDevice currentBinding(DeviceId deviceId) {
        return Fluxzero.loadCurrentGraph(deviceId).childModels(HomeAssistantDevice.class).stream().findFirst().orElse(null);
    }

    @HandleEvent
    void connected(ConnectHomeAssistant event) {
        var current = Fluxzero.loadCurrentGraph(event.connectionId()).get();
        if (current != null) Fluxzero.schedule(new RefreshHomeAssistant(event.connectionId()),
                refreshSchedule(event.connectionId()), Duration.ZERO);
    }

    @HandleEvent
    void linked(LinkHomeAssistantDevice event) {
        var binding = currentBinding(event.deviceId());
        if (binding == null) return;
        deliver(new DeliverHomeAssistantSettings(binding.deviceId(), binding.connectionId()));
        refresh(new RefreshHomeAssistant(binding.connectionId()));
    }

    @HandleEvent
    void changed(DeviceCommand event) {
        var binding = currentBinding(event.deviceId());
        if (binding != null) deliver(new DeliverHomeAssistantSettings(binding.deviceId(), binding.connectionId()));
    }

    @HandleEvent
    void bindingChanged(Graph<HomeAssistantDevice> graph) {
        if (graph.get() == null && graph.current().get() == null) {
            Fluxzero.cancelSchedule(deliverySchedule(new DeviceId(graph.functionalId())));
        }
    }

    // Scheduled work joins the same event consumer as device changes, so physical writes do not race across trackers.
    @HandleSchedule void refreshDue(RefreshHomeAssistant schedule) { Fluxzero.publishEvent(schedule); }
    @HandleSchedule void deliveryDue(DeliverHomeAssistantSettings schedule) { Fluxzero.publishEvent(schedule); }

    @HandleEvent
    void deliver(DeliverHomeAssistantSettings request) {
        var binding = currentBinding(request.deviceId());
        if (binding == null || !binding.connectionId().equals(request.connectionId())) return;
        var connection = Fluxzero.loadCurrentGraph(request.connectionId()).get();
        var device = Fluxzero.loadCurrentGraph(request.deviceId()).get();
        if (connection == null || device == null || device.desiredSettings().isEmpty()) return;
        try {
            var snapshot = Fluxzero.queryAndWait(new GetHomeAssistantStates(connection.connectionId()));
            for (var action : snapshot.actions(device, binding.entityIds())) {
                Fluxzero.sendCommandAndWait(new CallHomeAssistantService(connection.connectionId(), action));
            }
            Fluxzero.cancelSchedule(deliverySchedule(device.deviceId()));
            if (binding.problem() != null) Fluxzero.sendCommandAndWait(
                    new RecordHomeAssistantDeliveryProblem(device.deviceId(), connection.connectionId(), null));
        } catch (HomeAssistantUnavailable | IllegalCommandException failure) {
            Fluxzero.sendCommandAndWait(new RecordHomeAssistantDeliveryProblem(
                    device.deviceId(), connection.connectionId(), failure.getMessage()));
            Fluxzero.schedule(request, deliverySchedule(device.deviceId()), connection.refreshInterval());
        }
    }

    @HandleEvent
    void refresh(RefreshHomeAssistant request) {
        var connectionGraph = Fluxzero.loadCurrentGraph(request.connectionId());
        var connection = connectionGraph.get();
        if (connection == null) return;
        try {
            var snapshot = Fluxzero.queryAndWait(new GetHomeAssistantStates(connection.connectionId()));
            var sampledAt = Fluxzero.currentTime();
            for (var binding : connectionGraph.childModels(HomeAssistantDevice.class)) {
                var current = currentBinding(binding.deviceId());
                if (!binding.equals(current)) continue;
                var device = Fluxzero.loadCurrentGraph(binding.deviceId()).get();
                if (device == null) continue;
                var report = snapshot.observe(device, binding.entityIds(), sampledAt);
                var previous = Fluxzero.loadCurrentGraph(device.deviceId()).childModels(DeviceStatus.class).stream().findFirst().orElse(null);
                if (previous == null || previous.availability() != report.availability()
                        || !previous.reportedSettings().equals(report.reportedSettings())
                        || !previous.readings().equals(report.readings())) Fluxzero.sendCommandAndWait(
                                new AcceptHomeAssistantObservation(device.deviceId(), request.connectionId(), binding.entityIds(), report));
            }
            if (connection.problem() != null) Fluxzero.sendCommandAndWait(new RecordHomeAssistantProblem(request.connectionId(), null));
        } catch (HomeAssistantUnavailable failure) {
            Fluxzero.sendCommandAndWait(new RecordHomeAssistantProblem(request.connectionId(), failure.getMessage()));
        } finally {
            // An offline installation remains observable and can recover; deleting its parent cancels this work.
            var current = Fluxzero.loadCurrentGraph(request.connectionId()).get();
            if (current != null) Fluxzero.schedule(request, refreshSchedule(request.connectionId()), current.refreshInterval());
        }
    }
}
