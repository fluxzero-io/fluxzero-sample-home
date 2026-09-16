package io.fluxzero.home.automation.api;

import io.fluxzero.home.automation.api.model.Automation;
import io.fluxzero.sdk.modeling.Id;

/** Stable identity of a automation; its prefix prevents collisions with other kinds of model. */
public final class AutomationId extends Id<Automation> {
    public AutomationId(String value) { super(value, "automation:"); }
}
