package io.fluxzero.home.query;

import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceStatus;
import io.fluxzero.home.model.DeviceStatusId;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.tracking.handling.HandleQuery;
import io.fluxzero.sdk.tracking.handling.Request;

/** Read the latest evidence about a device without implying that desired settings were delivered. */
public record GetDeviceStatus(DeviceId deviceId) implements Request<DeviceStatus> {
    @HandleQuery DeviceStatus handle() { return Fluxzero.loadModel(new DeviceStatusId(deviceId.getFunctionalId())).get(); }
}
