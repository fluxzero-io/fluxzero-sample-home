package io.fluxzero.home.homeassistant.api.model;

import io.fluxzero.home.devices.api.ReportDeviceStatus;
import io.fluxzero.home.devices.api.model.Availability;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.devices.api.model.DeviceSettings;
import io.fluxzero.home.devices.api.model.Measurement;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/** One GET /api/states can assemble a complete observation from several entities without erasing sibling readings. */
public record HomeAssistantSnapshot(List<HomeAssistantState> states, String temperatureUnit) {
    public List<HomeAssistantEntity> discover() {
        return states.stream().map(HomeAssistantState::describe)
                .filter(e -> !e.capabilities().isEmpty() || !e.measurements().isEmpty()).toList();
    }

    public void validateBinding(Device device, Set<String> entityIds) {
        var capabilities = EnumSet.noneOf(Capability.class);
        var measurements = EnumSet.noneOf(Measurement.class);
        for (String id : entityIds) {
            var state = states.stream().filter(s -> s.entityId().equals(id)).findFirst()
                    .orElseThrow(() -> new IllegalCommandException("Choose existing Home Assistant entities."));
            var entity = state.describe();
            if (entity.capabilities().isEmpty() && entity.measurements().isEmpty()) {
                throw new IllegalCommandException("This Home Assistant entity is not supported by the example adapter.");
            }
            for (var capability : entity.capabilities()) if (!capabilities.add(capability)) {
                throw new IllegalCommandException("Choose one controlling entity for each device capability.");
            }
            for (var measurement : entity.measurements()) if (!measurements.add(measurement)) {
                throw new IllegalCommandException("Choose one sensor entity for each device measurement.");
            }
        }
        if (!capabilities.containsAll(device.capabilities()) || !measurements.containsAll(device.measurements())) {
            throw new IllegalCommandException("The selected entities do not cover this device's capabilities and measurements.");
        }
    }

    public ReportDeviceStatus observe(Device device, Set<String> entityIds, Instant sampledAt) {
        var settings = DeviceSettings.empty();
        var readings = new HashMap<Measurement, BigDecimal>();
        var availability = Availability.ONLINE;
        for (String id : entityIds) {
            var state = states.stream().filter(s -> s.entityId().equals(id)).findFirst().orElse(null);
            if (state == null || state.availability() == Availability.OFFLINE) {
                availability = Availability.OFFLINE;
                continue;
            }
            if (state.availability() == Availability.UNKNOWN && availability != Availability.OFFLINE) availability = Availability.UNKNOWN;
            for (var setting : state.settings(temperatureUnit).values()) {
                if (device.capabilities().contains(setting.capability())) settings = settings.with(setting);
            }
            var reading = state.reading();
            if (reading != null && device.measurements().contains(state.measurement())) readings.put(state.measurement(), reading);
        }
        if (availability == Availability.ONLINE && (!readings.keySet().containsAll(device.measurements())
                || !settings.values().stream().map(s -> s.capability()).toList().containsAll(device.capabilities()))) {
            availability = Availability.UNKNOWN;
        }
        return new ReportDeviceStatus(device.deviceId(), sampledAt, availability, settings, readings);
    }

    public List<HomeAssistantAction> actions(Device device, Set<String> entityIds) {
        validateBinding(device, entityIds);
        var result = new ArrayList<HomeAssistantAction>();
        for (var state : states) if (entityIds.contains(state.entityId())) {
            result.addAll(state.actions(device.pendingSettings(), temperatureUnit));
        }
        return result;
    }
}
