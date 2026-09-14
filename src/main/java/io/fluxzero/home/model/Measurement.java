package io.fluxzero.home.model;

import java.math.BigDecimal;

/** Measurements have one canonical unit, so rules never compare incompatible quantities. */
public enum Measurement {
    TEMPERATURE("°C"), HUMIDITY("%"), ILLUMINANCE("lx"), MOTION("boolean"), CONTACT_OPEN("boolean"),
    SMOKE("boolean"), WATER_LEAK("boolean"), POWER("W"), ENERGY("kWh"), BATTERY("%"), CARBON_DIOXIDE("ppm");
    private final String unit;
    Measurement(String unit) { this.unit = unit; }
    public String unit() { return unit; }
    /** Whether a value is meaningful in this measurement's canonical unit. */
    public boolean accepts(BigDecimal value) {
        if (value == null) return false;
        if (unit.equals("boolean")) return value.compareTo(BigDecimal.ZERO) == 0 || value.compareTo(BigDecimal.ONE) == 0;
        if (unit.equals("%")) return value.signum() >= 0 && value.compareTo(BigDecimal.valueOf(100)) <= 0;
        if (this == ILLUMINANCE || this == ENERGY || this == CARBON_DIOXIDE) return value.signum() >= 0;
        return true;
    }
}
