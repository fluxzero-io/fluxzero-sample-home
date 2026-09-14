package io.fluxzero.home.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** The readings before and after one committed observation from a device. */
public record DeviceObservationChanged(DeviceId deviceId, Instant at,
                                       Map<Measurement, BigDecimal> before,
                                       Map<Measurement, BigDecimal> after) implements HomeChange {}
