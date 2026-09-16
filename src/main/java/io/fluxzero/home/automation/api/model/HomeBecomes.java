package io.fluxzero.home.automation.api.model;

import io.fluxzero.home.household.api.model.HomeMode;
import jakarta.validation.constraints.NotNull;

/** React when the household enters the chosen mode. */
public record HomeBecomes(@NotNull HomeMode mode) implements AutomationTrigger {
    @Override
    public boolean matches(HomeChange change) {
        return change instanceof HomeModeChanged c && c.before() != mode && c.after() == mode;
    }
}
