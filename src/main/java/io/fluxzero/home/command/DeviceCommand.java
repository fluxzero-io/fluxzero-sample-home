package io.fluxzero.home.command;

import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;

/** Describes an intention for one device; concrete commands own their applies. */
public interface DeviceCommand {
    DeviceId deviceId();
    DeviceSetting setting();
}
