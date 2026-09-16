package io.fluxzero.home.devices.api;

import io.fluxzero.home.devices.api.model.DeviceStatus;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.tracking.handling.HandleQuery;
import io.fluxzero.sdk.tracking.handling.Request;

/** Read the latest evidence about a device without implying that desired settings were delivered. */
public record GetDeviceStatus(DeviceId deviceId) implements Request<DeviceStatus> {
    @HandleQuery DeviceStatus handle() { return Fluxzero.loadModel(deviceId, DeviceStatus.class).get(); }
}
