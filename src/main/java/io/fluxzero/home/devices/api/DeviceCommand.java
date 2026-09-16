package io.fluxzero.home.devices.api;

import io.fluxzero.home.devices.api.model.DeviceSetting;

/** Describes an intention for one device; concrete commands own their applies. */
public interface DeviceCommand {
    DeviceId deviceId();
    DeviceSetting setting();
}
