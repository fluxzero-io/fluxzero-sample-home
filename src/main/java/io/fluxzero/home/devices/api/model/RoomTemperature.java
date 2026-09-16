package io.fluxzero.home.devices.api.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** The requested or observed comfort temperature in degrees Celsius. */
public record RoomTemperature(@NotNull @DecimalMin("5") @DecimalMax("35") BigDecimal celsius) implements DeviceSetting {
    @Override
    public Capability capability() {
        return Capability.TEMPERATURE;
    }
}
