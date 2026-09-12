package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.Id;

/** Stable identity of a device; its prefix prevents collisions with other kinds of model. */
public final class DeviceId extends Id<Device> {
    public DeviceId(String value) { super(value, "device:"); }
}
