package io.fluxzero.home.automation.api.model;

import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.model.Measurement;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** The readings before and after one committed observation from a device. */
public record DeviceObservationChanged(DeviceId deviceId, Instant at,
                                       Map<Measurement, BigDecimal> before,
                                       Map<Measurement, BigDecimal> after) implements HomeChange {}
