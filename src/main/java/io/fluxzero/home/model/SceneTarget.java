package io.fluxzero.home.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.fluxzero.sdk.modeling.Graph;

import java.util.List;

/** A concrete selection of suitable devices within one home. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OneDevice.class, name = "device"),
    @JsonSubTypes.Type(value = InSpace.class, name = "space"),
    @JsonSubTypes.Type(value = InZone.class, name = "zone"),
    @JsonSubTypes.Type(value = WholeHome.class, name = "home")
})
public sealed interface SceneTarget permits OneDevice, InSpace, InZone, WholeHome {
    List<Device> select(Graph<Home> home, Capability capability);
}
