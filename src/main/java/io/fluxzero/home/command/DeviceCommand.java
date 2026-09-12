package io.fluxzero.home.command;

import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Shared pure behavior for the everyday commands that address one device. */
public interface DeviceCommand {
    DeviceId deviceId();
    DeviceSetting setting();
    @AssertLegal default void validate(Device device) { device.assertSupports(setting()); }
    @Apply default Device apply(Device device) { return device.request(setting()); }
    static DeviceCommand from(DeviceId id, DeviceSetting setting) {
        return switch (setting) {
            case DeviceSetting.Power s -> s.on() ? new TurnOn(id) : new TurnOff(id);
            case DeviceSetting.LightLevel s -> new DimLight(id, s.percent());
            case DeviceSetting.LightColor s -> new SetLightColor(id, s.hue(), s.saturation());
            case DeviceSetting.Temperature s -> new SetRoomTemperature(id, s.celsius());
            case DeviceSetting.Opening s -> new SetOpening(id, s.percent());
            case DeviceSetting.Lock s -> s.locked() ? new LockDoor(id) : new UnlockDoor(id);
            case DeviceSetting.Playback s -> s.playing() ? new PlayMedia(id, s.media()) : new StopMedia(id);
            case DeviceSetting.Volume s -> new SetVolume(id, s.percent());
            case DeviceSetting.FanSpeed s -> new SetFanSpeed(id, s.percent());
            case DeviceSetting.Irrigation s -> s.watering() ? new StartWatering(id) : new StopWatering(id);
            case DeviceSetting.Charging s -> s.enabled() ? new EnableCharging(id) : new PauseCharging(id);
        };
    }
}
