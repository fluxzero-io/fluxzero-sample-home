package io.fluxzero.home.automation.api.model;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.ZoneId;

/** One absolute moment; after it has passed, there is no next occurrence. */
public record Once(@NotNull Instant at) implements RoutineTiming {
    @Override
    @Nullable
    public Instant nextAfter(Instant after, ZoneId zone) {
        return at.isAfter(after) ? at : null;
    }
}
