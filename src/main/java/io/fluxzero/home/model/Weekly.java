package io.fluxzero.home.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

/** Selected weekdays at a local time; skip clock gaps and use only the first occurrence of an overlap. */
public record Weekly(@NotEmpty(message = "Choose at least one day of the week.") Set<@NotNull DayOfWeek> days,
                     @NotNull LocalTime time) implements RoutineTiming {
    @Override
    public Instant nextAfter(Instant after, ZoneId zone) {
        var start = after.atZone(zone).toLocalDate();
        // Include the following week when this week's chosen local time falls in a clock gap.
        for (int offset = 0; offset <= 14; offset++) {
            var date = start.plusDays(offset);
            if (!days.contains(date.getDayOfWeek())) {
                continue;
            }
            var local = date.atTime(time);
            var offsets = zone.getRules().getValidOffsets(local);
            if (offsets.isEmpty()) {
                continue;
            }
            var candidate = local.toInstant(offsets.getFirst());
            if (candidate.isAfter(after)) {
                return candidate;
            }
        }
        throw new HomeRuleViolation("No upcoming moment exists for this weekly routine.");
    }
}
