package io.fluxzero.home.command;

import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;

/** Ask a climate device for a comfortable room temperature. */
public record SetRoomTemperature(DeviceId deviceId, java.math.BigDecimal celsius) implements DeviceCommand {
    public DeviceSetting setting() { return new DeviceSetting.Temperature(celsius); }
}
