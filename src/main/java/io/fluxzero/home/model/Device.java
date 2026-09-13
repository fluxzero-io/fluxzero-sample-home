package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.Alias;
import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import lombok.With;

/** A named device and the desired settings requested by the household. */
@Model
@With
public record Device(@EntityId DeviceId deviceId,
                     @Parent(pathInParent = "devices") SpaceId spaceId,
                     String name, @Alias(prefix = "device-label:") String label,
                     Set<Capability> capabilities, Set<Measurement> measurements,
                     Map<Capability, DeviceSetting> desiredSettings) {
    public Device {
        capabilities = Set.copyOf(capabilities);
        measurements = Set.copyOf(measurements);
        desiredSettings = Map.copyOf(desiredSettings);
    }
    public void assertSupports(DeviceSetting setting) {
        setting.validate();
        Rules.require(capabilities.contains(setting.capability()), name + " cannot perform " + setting.capability() + ".");
    }
    public Device request(DeviceSetting setting) {
        assertSupports(setting);
        var updated = new EnumMap<Capability, DeviceSetting>(Capability.class);
        updated.putAll(desiredSettings);
        updated.put(setting.capability(), setting);
        return withDesiredSettings(updated);
    }
}
