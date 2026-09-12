package io.fluxzero.home.model;

import java.math.BigDecimal;

/** Measurements have one canonical unit, so rules never compare incompatible quantities. */
public enum Measurement {
    TEMPERATURE("°C"), HUMIDITY("%"), ILLUMINANCE("lx"), MOTION("boolean"), CONTACT_OPEN("boolean"),
    SMOKE("boolean"), WATER_LEAK("boolean"), POWER("W"), ENERGY("kWh"), BATTERY("%"), CARBON_DIOXIDE("ppm");
    private final String unit;
    Measurement(String unit) { this.unit = unit; }
    public String unit() { return unit; }
    public void validate(BigDecimal value) {
        Rules.require(value != null, "A measurement needs a value.");
        if (unit.equals("boolean")) Rules.require(value.compareTo(BigDecimal.ZERO) == 0 || value.compareTo(BigDecimal.ONE) == 0,
                "A yes/no measurement must be zero or one.");
        if (unit.equals("%")) Rules.require(value.signum() >= 0 && value.compareTo(new BigDecimal("100")) <= 0,
                "A percentage measurement must be between zero and 100.");
        if (this == ILLUMINANCE || this == ENERGY || this == CARBON_DIOXIDE)
            Rules.require(value.signum() >= 0, "This measurement cannot be negative.");
    }
}
