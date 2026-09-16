package io.fluxzero.home.devices.api.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** A typed setting; concrete values own their input constraints. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Power.class, name = "power"),
    @JsonSubTypes.Type(value = LightLevel.class, name = "lightLevel"),
    @JsonSubTypes.Type(value = LightColor.class, name = "lightColor"),
    @JsonSubTypes.Type(value = RoomTemperature.class, name = "temperature"),
    @JsonSubTypes.Type(value = Opening.class, name = "opening"),
    @JsonSubTypes.Type(value = DoorLock.class, name = "lock"),
    @JsonSubTypes.Type(value = Playback.class, name = "playback"),
    @JsonSubTypes.Type(value = Volume.class, name = "volume"),
    @JsonSubTypes.Type(value = FanSpeed.class, name = "fanSpeed"),
    @JsonSubTypes.Type(value = Irrigation.class, name = "irrigation"),
    @JsonSubTypes.Type(value = Charging.class, name = "charging")
})
public sealed interface DeviceSetting permits Power, LightLevel, LightColor, RoomTemperature, Opening, DoorLock,
        Playback, Volume, FanSpeed, Irrigation, Charging {
    Capability capability();
}
