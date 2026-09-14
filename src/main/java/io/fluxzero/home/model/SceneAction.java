package io.fluxzero.home.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.fluxzero.home.command.DeviceCommand;

/** A concrete household intention that can address a selection of devices. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SwitchPower.class, name = "switchPower"),
    @JsonSubTypes.Type(value = DimLights.class, name = "dimLights"),
    @JsonSubTypes.Type(value = ColorLights.class, name = "colorLights"),
    @JsonSubTypes.Type(value = SetHeating.class, name = "setHeating"),
    @JsonSubTypes.Type(value = PositionCoverings.class, name = "positionCoverings"),
    @JsonSubTypes.Type(value = SetLocks.class, name = "setLocks"),
    @JsonSubTypes.Type(value = SetPlayback.class, name = "setPlayback"),
    @JsonSubTypes.Type(value = AdjustVolume.class, name = "adjustVolume"),
    @JsonSubTypes.Type(value = SetVentilation.class, name = "setVentilation"),
    @JsonSubTypes.Type(value = SetWatering.class, name = "setWatering"),
    @JsonSubTypes.Type(value = SetCharging.class, name = "setCharging")
})
public sealed interface SceneAction permits SwitchPower, DimLights, ColorLights, SetHeating, PositionCoverings,
        SetLocks, SetPlayback, AdjustVolume, SetVentilation, SetWatering, SetCharging {
    SceneTarget target();
    Capability capability();
    DeviceCommand commandFor(DeviceId deviceId);
}
