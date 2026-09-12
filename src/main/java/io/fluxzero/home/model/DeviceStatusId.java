package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.Id;

/** Stable identity of a devicestatus; its prefix prevents collisions with other kinds of model. */
public final class DeviceStatusId extends Id<DeviceStatus> {
    public DeviceStatusId(String value) { super(value, "devicestatus:"); }
}
