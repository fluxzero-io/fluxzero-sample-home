package io.fluxzero.home.homeassistant.api;

import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.sdk.modeling.Parent;

/** Re-read the current intention at delivery time; the schedule never contains an old setting. */
public record DeliverHomeAssistantSettings(@Parent DeviceId deviceId, @Parent HomeAssistantId connectionId) {}
