package io.fluxzero.home.access;

import io.fluxzero.home.model.HomeId;
import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;

import java.util.Map;

/** Application-owned access, separate from a resident's presence or household role. */
@Model
public record Account(@EntityId AccountId accountId, AccountDetails details,
                      Map<HomeId, HomePermission> homes) {}
