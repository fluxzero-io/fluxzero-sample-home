package io.fluxzero.home.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** At most one value per capability, in a stable order independent of how it was assembled. */
public record DeviceSettings(@NotNull @Valid List<@NotNull DeviceSetting> values) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public DeviceSettings {
        values = values == null ? null : values.stream()
                .sorted(Comparator.nullsLast(Comparator.comparing(DeviceSetting::capability)))
                .toList();
    }

    public static DeviceSettings empty() {
        return new DeviceSettings(List.of());
    }

    @Override
    @JsonValue
    public List<DeviceSetting> values() {
        return values;
    }

    @AssertTrue(message = "Report at most one setting for each capability.")
    boolean hasDistinctCapabilities() {
        return values == null || values.stream().filter(Objects::nonNull)
                .map(DeviceSetting::capability).distinct().count()
                == values.stream().filter(Objects::nonNull).count();
    }

    public DeviceSettings with(DeviceSetting setting) {
        var updated = new ArrayList<>(values);
        updated.removeIf(current -> current.capability() == setting.capability());
        updated.add(setting);
        return new DeviceSettings(updated);
    }

    public DeviceSetting get(Capability capability) {
        return values.stream().filter(setting -> setting.capability() == capability).findFirst().orElse(null);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }
}
