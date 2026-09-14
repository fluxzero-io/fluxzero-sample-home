package io.fluxzero.home.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;

/** A one-off moment or a weekly local-time rhythm, independent of machine time. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = RoutineTiming.Once.class, name = "once"),
    @JsonSubTypes.Type(value = RoutineTiming.Weekly.class, name = "weekly")
})
public sealed interface RoutineTiming {
    Instant nextAfter(Instant after, java.time.ZoneId zone);
    record Once(@NotNull Instant at) implements RoutineTiming {
        public Instant nextAfter(Instant after, java.time.ZoneId zone) { return at.isAfter(after) ? at : null; }
    }
    /** Missing spring-clock times are skipped; an autumn overlap executes at its first occurrence only. */
    record Weekly(@NotEmpty(message = "Choose at least one day of the week.") Set<@NotNull DayOfWeek> days,
                  @NotNull LocalTime time) implements RoutineTiming {
        public Instant nextAfter(Instant after, java.time.ZoneId zone) {
            var start = after.atZone(zone).toLocalDate();
            // Fourteen days also cover a chosen weekday whose clock time does not exist this week.
            for (int offset = 0; offset <= 14; offset++) {
                var date = start.plusDays(offset);
                if (!days.contains(date.getDayOfWeek())) continue;
                var local = date.atTime(time);
                var offsets = zone.getRules().getValidOffsets(local);
                if (offsets.isEmpty()) continue;
                var candidate = local.toInstant(offsets.getFirst());
                if (candidate.isAfter(after)) return candidate;
            }
            throw new HomeRuleViolation("No upcoming moment exists for this weekly routine.");
        }
    }
}
