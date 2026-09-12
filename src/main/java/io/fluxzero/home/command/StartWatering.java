package io.fluxzero.home.command;

import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;

/** Start watering a garden area. */
public record StartWatering(DeviceId deviceId) implements DeviceCommand {
    public DeviceSetting setting() { return new DeviceSetting.Irrigation(true); }
}
