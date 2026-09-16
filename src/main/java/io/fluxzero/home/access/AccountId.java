package io.fluxzero.home.access;

import io.fluxzero.sdk.modeling.Id;

/** The subject issued by this installation's configured identity provider. */
public final class AccountId extends Id<Account> {
    public AccountId(String value) { super(value, "account:"); }
}
