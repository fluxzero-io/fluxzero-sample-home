package io.fluxzero.home.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** A scene can address one device, a space and its contents, a zone, or the whole home. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SceneTarget.OneDevice.class, name = "device"),
    @JsonSubTypes.Type(value = SceneTarget.InSpace.class, name = "space"),
    @JsonSubTypes.Type(value = SceneTarget.InZone.class, name = "zone"),
    @JsonSubTypes.Type(value = SceneTarget.WholeHome.class, name = "home")
})
public sealed interface SceneTarget {
    record OneDevice(DeviceId deviceId) implements SceneTarget {}
    record InSpace(SpaceId spaceId) implements SceneTarget {}
    record InZone(ZoneId zoneId) implements SceneTarget {}
    record WholeHome() implements SceneTarget {}
}
