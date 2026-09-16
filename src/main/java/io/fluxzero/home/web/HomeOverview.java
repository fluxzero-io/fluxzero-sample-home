package io.fluxzero.home.web;

import io.fluxzero.home.access.HomePermission;
import io.fluxzero.home.homeassistant.HomeAssistantDevice;
import io.fluxzero.home.model.Automation;
import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.DeviceStatus;
import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.Resident;
import io.fluxzero.home.model.Routine;
import io.fluxzero.home.model.Scene;
import io.fluxzero.home.model.Space;
import io.fluxzero.home.model.Zone;
import io.fluxzero.sdk.modeling.Graph;

import java.util.List;

/** A complete household snapshot from one pinned Graph; observations remain separate. */
public record HomeOverview(Home home, HomePermission permission, List<Space> spaces, List<DeviceView> devices,
                           List<Scene> scenes, List<Routine> routines, List<Automation> automations,
                           List<Zone> zones, List<Resident> residents) {
    public static HomeOverview from(Graph<Home> home, HomePermission permission) {
        return new HomeOverview(home.get(), permission, home.descendantModels(Space.class),
                home.descendants(Device.class).stream().map(device -> new DeviceView(device.get(),
                        device.childModels(DeviceStatus.class).stream().findFirst().orElse(null),
                        device.childModels(HomeAssistantDevice.class).stream().findFirst()
                                .map(link -> new Delivery("Home Assistant", link.problem())).orElse(null))).toList(),
                home.childModels(Scene.class), home.childModels(Routine.class), home.childModels(Automation.class),
                home.childModels(Zone.class), home.childModels(Resident.class));
    }
    public record DeviceView(Device device, DeviceStatus status, Delivery delivery) {}
    public record Delivery(String integration, String problem) {}
}
