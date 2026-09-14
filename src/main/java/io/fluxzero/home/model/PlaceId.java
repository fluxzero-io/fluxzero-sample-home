package io.fluxzero.home.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fluxzero.sdk.modeling.Id;

/**
 * A destination within the physical layout: either a home or another space.
 * Jackson resolves the concrete subtype before constructing its ID; the pinned SDK's ID deserializer
 * treats this abstract base as a concrete ID. Concrete ID fields retain their scalar JSON form.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_ARRAY)
@JsonDeserialize(using = JsonDeserializer.None.class)
@JsonSubTypes({
    @JsonSubTypes.Type(value = HomeId.class, name = "home"),
    @JsonSubTypes.Type(value = SpaceId.class, name = "space")
})
public abstract sealed class PlaceId<T extends Place> extends Id<T> permits HomeId, SpaceId {
    protected PlaceId(String value, String prefix) {
        super(value, prefix);
    }
}
