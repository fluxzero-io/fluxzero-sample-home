package io.fluxzero.home.homeassistant.api.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.fluxzero.home.devices.api.model.Availability;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.DeviceSettings;
import io.fluxzero.home.devices.api.model.LightColor;
import io.fluxzero.home.devices.api.model.LightLevel;
import io.fluxzero.home.devices.api.model.Measurement;
import io.fluxzero.home.devices.api.model.Opening;
import io.fluxzero.home.devices.api.model.Power;
import io.fluxzero.home.devices.api.model.RoomTemperature;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Translation of the documented HA entity state. Protocol attributes stay at this boundary. */
public record HomeAssistantState(String entityId, String state, JsonNode attributes) {
    public static HomeAssistantState from(JsonNode value) {
        if (!value.path("entity_id").isTextual() || !value.path("state").isTextual()
                || !value.path("attributes").isObject()) throw new IllegalArgumentException("Invalid entity state");
        return new HomeAssistantState(value.get("entity_id").asText(), value.get("state").asText(), value.get("attributes"));
    }

    private String domain() { return entityId.split("\\.", 2)[0]; }

    public HomeAssistantEntity describe() {
        var measurement = measurement();
        return new HomeAssistantEntity(entityId, attributes.path("friendly_name").asText(entityId), capabilities(),
                measurement == null ? Set.of() : Set.of(measurement));
    }

    Set<Capability> capabilities() {
        return switch (domain()) {
            case "light" -> {
                var result = EnumSet.of(Capability.POWER);
                if (supportsBrightness()) result.add(Capability.LIGHT_LEVEL);
                if (supportsColor()) result.add(Capability.LIGHT_COLOR);
                yield result;
            }
            case "switch" -> Set.of(Capability.POWER);
            case "climate" -> supportsFeature(1) ? Set.of(Capability.TEMPERATURE) : Set.of(); // TARGET_TEMPERATURE
            case "cover" -> supportsFeature(4) ? Set.of(Capability.OPENING) : Set.of(); // SET_POSITION
            default -> Set.of();
        };
    }

    private boolean supportsBrightness() {
        for (var mode : attributes.path("supported_color_modes")) {
            if (Set.of("brightness", "color_temp", "hs", "xy", "rgb", "rgbw", "rgbww", "white").contains(mode.asText())) return true;
        }
        return false;
    }

    private boolean supportsColor() {
        for (var mode : attributes.path("supported_color_modes")) {
            if (Set.of("hs", "xy", "rgb", "rgbw", "rgbww").contains(mode.asText())) return true;
        }
        return false;
    }

    private boolean supportsFeature(int feature) {
        return (attributes.path("supported_features").asInt() & feature) != 0;
    }

    Availability availability() {
        return switch (state) {
            case "unavailable" -> Availability.OFFLINE;
            case "unknown" -> Availability.UNKNOWN;
            default -> Availability.ONLINE;
        };
    }

    DeviceSettings settings(String temperatureUnit) {
        var result = DeviceSettings.empty();
        if (availability() != Availability.ONLINE) return result;
        if (domain().equals("climate")) {
            if (capabilities().contains(Capability.TEMPERATURE) && !state.equals("heat_cool")
                    && attributes.path("temperature").isNumber()) {
                var temperature = toCelsius(attributes.get("temperature").decimalValue(), temperatureUnit);
                if (temperature.compareTo(BigDecimal.valueOf(5)) >= 0 && temperature.compareTo(BigDecimal.valueOf(35)) <= 0) {
                    return result.with(new RoomTemperature(temperature));
                }
            }
            return result;
        }
        if (domain().equals("cover")) {
            var position = attributes.path("current_position");
            return capabilities().contains(Capability.OPENING) && position.isIntegralNumber()
                    && position.asInt() >= 0 && position.asInt() <= 100
                    ? result.with(new Opening(position.asInt())) : result;
        }
        if (!capabilities().contains(Capability.POWER) || !(state.equals("on") || state.equals("off"))) return result;
        result = result.with(new Power(state.equals("on")));
        if (supportsBrightness()) {
            if (state.equals("off")) result = result.with(new LightLevel(0));
            else if (attributes.path("brightness").isNumber()) {
                int brightness = attributes.get("brightness").asInt();
                if (brightness >= 0 && brightness <= 255) result = result.with(new LightLevel(Math.round(brightness * 100f / 255)));
            }
        }
        var color = attributes.path("hs_color");
        if (supportsColor() && color.isArray() && color.size() == 2 && color.get(0).isNumber() && color.get(1).isNumber()) {
            double hue = color.get(0).asDouble(), saturation = color.get(1).asDouble();
            if (hue >= 0 && hue <= 360 && saturation >= 0 && saturation <= 100) {
                result = result.with(new LightColor((int) Math.round(hue) % 360, (int) Math.round(saturation)));
            }
        }
        return result;
    }

    Measurement measurement() {
        String kind = attributes.path("device_class").asText();
        if (domain().equals("binary_sensor")) return switch (kind) {
            case "motion", "occupancy", "presence" -> Measurement.MOTION;
            case "door", "window", "opening" -> Measurement.CONTACT_OPEN;
            case "smoke" -> Measurement.SMOKE;
            case "moisture" -> Measurement.WATER_LEAK;
            default -> null;
        };
        if (!domain().equals("sensor")) return null;
        String unit = attributes.path("unit_of_measurement").asText();
        return switch (kind) {
            case "temperature" -> Set.of("°C", "°F").contains(unit) ? Measurement.TEMPERATURE : null;
            case "humidity" -> unit.equals("%") ? Measurement.HUMIDITY : null;
            case "illuminance" -> unit.equals("lx") ? Measurement.ILLUMINANCE : null;
            case "power" -> Set.of("W", "kW").contains(unit) ? Measurement.POWER : null;
            case "energy" -> Set.of("Wh", "kWh").contains(unit) ? Measurement.ENERGY : null;
            case "battery" -> unit.equals("%") ? Measurement.BATTERY : null;
            case "carbon_dioxide" -> unit.equals("ppm") ? Measurement.CARBON_DIOXIDE : null;
            default -> null;
        };
    }

    BigDecimal reading() {
        var measurement = measurement();
        if (measurement == null || availability() != Availability.ONLINE) return null;
        if (domain().equals("binary_sensor")) return switch (state) {
            case "on" -> BigDecimal.ONE;
            case "off" -> BigDecimal.ZERO;
            default -> null;
        };
        try {
            var value = new BigDecimal(state);
            value = switch (attributes.path("unit_of_measurement").asText()) {
                case "°F" -> value.subtract(BigDecimal.valueOf(32)).multiply(BigDecimal.valueOf(5))
                        .divide(BigDecimal.valueOf(9), 4, RoundingMode.HALF_UP).stripTrailingZeros();
                case "kW" -> value.multiply(BigDecimal.valueOf(1000));
                case "Wh" -> value.movePointLeft(3);
                default -> value;
            };
            return measurement.accepts(value) ? value : null;
        } catch (NumberFormatException invalid) { return null; }
    }

    List<HomeAssistantAction> actions(DeviceSettings desired, String temperatureUnit) {
        if (capabilities().isEmpty()) return List.of();
        if (availability() != Availability.ONLINE) throw new HomeAssistantUnavailable("A linked entity is unavailable in Home Assistant.");
        if (domain().equals("climate")) return temperatureActions(desired, temperatureUnit);
        if (domain().equals("cover")) {
            var opening = (Opening) desired.get(Capability.OPENING);
            return opening == null ? List.of() : List.of(new HomeAssistantAction("cover", "set_cover_position",
                    new HomeAssistantAction.SetPosition(entityId, opening.percent())));
        }
        var power = (Power) desired.get(Capability.POWER);
        var level = capabilities().contains(Capability.LIGHT_LEVEL) ? (LightLevel) desired.get(Capability.LIGHT_LEVEL) : null;
        var color = supportsColor() ? (LightColor) desired.get(Capability.LIGHT_COLOR) : null;
        if (power == null && level == null && color == null) return List.of();
        if (power != null && !power.on() || level != null && level.percent() == 0) {
            return List.of(new HomeAssistantAction(domain(), "turn_off", new HomeAssistantAction.SwitchEntity(entityId)));
        }
        if (color != null) return List.of(new HomeAssistantAction("light", "turn_on",
                new HomeAssistantAction.ColorEntity(entityId, List.of(color.hue(), color.saturation()),
                        level == null ? null : level.percent())));
        if (level != null) return List.of(new HomeAssistantAction("light", "turn_on", new HomeAssistantAction.DimEntity(entityId, level.percent())));
        return List.of(new HomeAssistantAction(domain(), "turn_on", new HomeAssistantAction.SwitchEntity(entityId)));
    }

    private List<HomeAssistantAction> temperatureActions(DeviceSettings desired, String temperatureUnit) {
        var setting = (RoomTemperature) desired.get(Capability.TEMPERATURE);
        if (setting == null) return List.of();
        if (state.equals("heat_cool")) {
            throw new HomeAssistantUnavailable("Choose a single-temperature mode in Home Assistant before setting the temperature.");
        }
        var temperature = switch (temperatureUnit) {
            case "°C" -> setting.celsius();
            case "°F" -> setting.celsius().multiply(BigDecimal.valueOf(1.8)).add(BigDecimal.valueOf(32));
            default -> throw new HomeAssistantUnavailable("Home Assistant did not report a supported temperature unit.");
        };
        var min = attributes.path("min_temp");
        var max = attributes.path("max_temp");
        if (min.isNumber() && temperature.compareTo(min.decimalValue()) < 0
                || max.isNumber() && temperature.compareTo(max.decimalValue()) > 0) {
            throw new HomeAssistantUnavailable("The temperature is outside this thermostat's supported range.");
        }
        return List.of(new HomeAssistantAction("climate", "set_temperature",
                new HomeAssistantAction.SetTemperature(entityId, temperature)));
    }

    private static BigDecimal toCelsius(BigDecimal temperature, String unit) {
        return switch (unit) {
            case "°C" -> temperature;
            case "°F" -> temperature.subtract(BigDecimal.valueOf(32)).multiply(BigDecimal.valueOf(5))
                    .divide(BigDecimal.valueOf(9), 4, RoundingMode.HALF_UP).stripTrailingZeros();
            default -> throw new HomeAssistantUnavailable("Home Assistant did not report a supported temperature unit.");
        };
    }
}
