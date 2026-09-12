package io.fluxzero.home.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.fluxzero.sdk.modeling.AssertLegal;
import java.math.BigDecimal;

/** Typed intentions used by both direct commands and reusable scenes. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DeviceSetting.Power.class, name = "power"),
    @JsonSubTypes.Type(value = DeviceSetting.LightLevel.class, name = "lightLevel"),
    @JsonSubTypes.Type(value = DeviceSetting.LightColor.class, name = "lightColor"),
    @JsonSubTypes.Type(value = DeviceSetting.Temperature.class, name = "temperature"),
    @JsonSubTypes.Type(value = DeviceSetting.Opening.class, name = "opening"),
    @JsonSubTypes.Type(value = DeviceSetting.Lock.class, name = "lock"),
    @JsonSubTypes.Type(value = DeviceSetting.Playback.class, name = "playback"),
    @JsonSubTypes.Type(value = DeviceSetting.Volume.class, name = "volume"),
    @JsonSubTypes.Type(value = DeviceSetting.FanSpeed.class, name = "fanSpeed"),
    @JsonSubTypes.Type(value = DeviceSetting.Irrigation.class, name = "irrigation"),
    @JsonSubTypes.Type(value = DeviceSetting.Charging.class, name = "charging")
})
public sealed interface DeviceSetting {
    Capability capability();
    @AssertLegal
    default void validate() {}
    static void percent(int value) { Rules.require(value >= 0 && value <= 100, "Choose a percentage from 0 to 100."); }
    record Power(boolean on) implements DeviceSetting {
        public Capability capability() { return Capability.POWER; }
    }
    record LightLevel(int percent) implements DeviceSetting {
        public Capability capability() { return Capability.LIGHT_LEVEL; }
        public void validate() { DeviceSetting.percent(percent); }
    }
    record LightColor(int hue, int saturation) implements DeviceSetting {
        public Capability capability() { return Capability.LIGHT_COLOR; }
        public void validate() { Rules.require(hue >= 0 && hue < 360, "Choose a hue from 0 to 359."); DeviceSetting.percent(saturation); }
    }
    record Temperature(BigDecimal celsius) implements DeviceSetting {
        public Capability capability() { return Capability.TEMPERATURE; }
        public void validate() { Rules.require(celsius != null && celsius.compareTo(new BigDecimal("5")) >= 0
                && celsius.compareTo(new BigDecimal("35")) <= 0, "Choose a room temperature from 5 to 35 degrees Celsius."); }
    }
    record Opening(int percent) implements DeviceSetting {
        public Capability capability() { return Capability.OPENING; }
        public void validate() { DeviceSetting.percent(percent); }
    }
    record Lock(boolean locked) implements DeviceSetting {
        public Capability capability() { return Capability.LOCK; }
    }
    record Playback(boolean playing, String media) implements DeviceSetting {
        public Capability capability() { return Capability.PLAYBACK; }
        public void validate() { Rules.require(!playing || media != null && !media.isBlank(), "Choose what to play."); }
    }
    record Volume(int percent) implements DeviceSetting {
        public Capability capability() { return Capability.VOLUME; }
        public void validate() { DeviceSetting.percent(percent); }
    }
    record FanSpeed(int percent) implements DeviceSetting {
        public Capability capability() { return Capability.FAN_SPEED; }
        public void validate() { DeviceSetting.percent(percent); }
    }
    record Irrigation(boolean watering) implements DeviceSetting {
        public Capability capability() { return Capability.IRRIGATION; }
    }
    record Charging(boolean enabled) implements DeviceSetting {
        public Capability capability() { return Capability.CHARGING; }
    }
}
