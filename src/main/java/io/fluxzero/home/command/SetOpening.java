package io.fluxzero.home.command;

import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;

/** Choose how far a blind, curtain or window should open. */
public record SetOpening(DeviceId deviceId, int percent) implements DeviceCommand {
    public DeviceSetting setting() { return new DeviceSetting.Opening(percent); }
}
