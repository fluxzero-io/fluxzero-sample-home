package io.fluxzero.home.homeassistant;

import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.Home;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.tracking.TrackSelf;
import io.fluxzero.sdk.tracking.handling.Association;
import io.fluxzero.sdk.tracking.handling.HandleCommand;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Set;

/** Explicitly choose the external entities that belong to an existing household device. */
@TrackSelf
public record LinkHomeAssistantDevice(DeviceId deviceId, HomeAssistantId connectionId,
                                      @NotEmpty Set<@NotNull @Pattern(regexp = "[a-z_]+\\.[a-z0-9_]+") String> entityIds) {
    @HandleCommand
    void handle() {
        Fluxzero.assertLegal(this);
        var device = Fluxzero.loadModel(deviceId).get();
        Fluxzero.queryAndWait(new GetHomeAssistantStates(connectionId)).validateBinding(device, entityIds);
        Fluxzero.assertAndApply(this);
    }

    @AssertLegal
    void belongsToSameHome(Device device, HomeAssistantConnection connection, @Association("homeAssistants") Graph<Home> home) {
        if (home.find(deviceId, Device.class).isEmpty()) {
            throw new IllegalCommandException("Choose a device and a Home Assistant connection from the same home.");
        }
    }

    @Apply
    HomeAssistantDevice apply(Device device, HomeAssistantConnection connection) {
        return new HomeAssistantDevice(deviceId, connectionId, entityIds, null);
    }
}
