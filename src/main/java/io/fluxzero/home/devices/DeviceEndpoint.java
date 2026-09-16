package io.fluxzero.home.devices;

import io.fluxzero.home.access.BrowserRequests;
import io.fluxzero.home.access.HomeAccess;
import io.fluxzero.home.access.api.model.HomePermission;
import io.fluxzero.home.access.api.model.HomeUser;
import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.DimLight;
import io.fluxzero.home.devices.api.EnableCharging;
import io.fluxzero.home.devices.api.LockDoor;
import io.fluxzero.home.devices.api.PauseCharging;
import io.fluxzero.home.devices.api.PlayMedia;
import io.fluxzero.home.devices.api.SetFanSpeed;
import io.fluxzero.home.devices.api.SetLightColor;
import io.fluxzero.home.devices.api.SetOpening;
import io.fluxzero.home.devices.api.SetRoomTemperature;
import io.fluxzero.home.devices.api.SetVolume;
import io.fluxzero.home.devices.api.StartWatering;
import io.fluxzero.home.devices.api.StopMedia;
import io.fluxzero.home.devices.api.StopWatering;
import io.fluxzero.home.devices.api.TurnOff;
import io.fluxzero.home.devices.api.TurnOn;
import io.fluxzero.home.devices.api.UnlockDoor;
import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.devices.api.model.LightLevel;
import io.fluxzero.home.devices.api.model.RoomTemperature;
import io.fluxzero.home.household.api.HomeId;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.tracking.handling.authentication.RequiresUser;
import io.fluxzero.sdk.tracking.handling.authentication.UnauthorizedException;
import io.fluxzero.sdk.web.ApiDoc;
import io.fluxzero.sdk.web.HandlePost;
import io.fluxzero.sdk.web.Path;
import io.fluxzero.sdk.web.PathParam;
import io.fluxzero.sdk.web.WebRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
@RequiresUser
@ApiDoc
@Path("/api/homes/{homeId}/devices/{deviceId}")
public class DeviceEndpoint {
    @HandlePost("/on") @ApiDoc(operationId = "turnOn")
    void turnOn(@PathParam HomeId homeId, @PathParam DeviceId deviceId, HomeUser user, WebRequest request) {
        requireControl(homeId, deviceId, user, request);
        Fluxzero.sendCommandAndWait(new TurnOn(deviceId));
    }

    @HandlePost("/off") @ApiDoc(operationId = "turnOff")
    void turnOff(@PathParam HomeId homeId, @PathParam DeviceId deviceId, HomeUser user, WebRequest request) {
        requireControl(homeId, deviceId, user, request);
        Fluxzero.sendCommandAndWait(new TurnOff(deviceId));
    }

    @HandlePost("/brightness") @ApiDoc(operationId = "dimLight")
    void dimLight(@PathParam HomeId homeId, @PathParam DeviceId deviceId, @Valid Percent value, HomeUser user, WebRequest request) {
        requireControl(homeId, deviceId, user, request);
        Fluxzero.sendCommandAndWait(new DimLight(deviceId, new LightLevel(value.percent())));
    }

    @HandlePost("/color") @ApiDoc(operationId = "setLightColor")
    void setLightColor(@PathParam HomeId homeId, @PathParam DeviceId deviceId, @Valid Color value, HomeUser user, WebRequest request) {
        requireControl(homeId, deviceId, user, request);
        Fluxzero.sendCommandAndWait(new SetLightColor(deviceId, value.hue(), value.saturation()));
    }

    @HandlePost("/temperature") @ApiDoc(operationId = "setRoomTemperature")
    void setRoomTemperature(@PathParam HomeId homeId, @PathParam DeviceId deviceId, @Valid Temperature value, HomeUser user, WebRequest request) {
        requireControl(homeId, deviceId, user, request);
        Fluxzero.sendCommandAndWait(new SetRoomTemperature(deviceId, new RoomTemperature(value.celsius())));
    }

    @HandlePost("/opening") @ApiDoc(operationId = "setOpening")
    void setOpening(@PathParam HomeId homeId, @PathParam DeviceId deviceId, @Valid Percent value, HomeUser user, WebRequest request) {
        requireControl(homeId, deviceId, user, request);
        Fluxzero.sendCommandAndWait(new SetOpening(deviceId, value.percent()));
    }

    @HandlePost("/lock") @ApiDoc(operationId = "lockDoor")
    void lockDoor(@PathParam HomeId homeId, @PathParam DeviceId deviceId, HomeUser user, WebRequest request) {
        requireControl(homeId, deviceId, user, request);
        Fluxzero.sendCommandAndWait(new LockDoor(deviceId));
    }

    @HandlePost("/unlock") @ApiDoc(operationId = "unlockDoor")
    void unlockDoor(@PathParam HomeId homeId, @PathParam DeviceId deviceId, HomeUser user, WebRequest request) {
        requireControl(homeId, deviceId, user, request);
        Fluxzero.sendCommandAndWait(new UnlockDoor(deviceId));
    }

    @HandlePost("/play") @ApiDoc(operationId = "playMedia")
    void playMedia(@PathParam HomeId homeId, @PathParam DeviceId deviceId, @Valid Media value, HomeUser user, WebRequest request) {
        requireControl(homeId, deviceId, user, request);
        Fluxzero.sendCommandAndWait(new PlayMedia(deviceId, value.media()));
    }

    @HandlePost("/stop") @ApiDoc(operationId = "stopMedia")
    void stopMedia(@PathParam HomeId homeId, @PathParam DeviceId deviceId, HomeUser user, WebRequest request) {
        requireControl(homeId, deviceId, user, request);
        Fluxzero.sendCommandAndWait(new StopMedia(deviceId));
    }

    @HandlePost("/volume") @ApiDoc(operationId = "setVolume")
    void setVolume(@PathParam HomeId homeId, @PathParam DeviceId deviceId, @Valid Percent value, HomeUser user, WebRequest request) {
        requireControl(homeId, deviceId, user, request);
        Fluxzero.sendCommandAndWait(new SetVolume(deviceId, value.percent()));
    }

    @HandlePost("/fan-speed") @ApiDoc(operationId = "setFanSpeed")
    void setFanSpeed(@PathParam HomeId homeId, @PathParam DeviceId deviceId, @Valid Percent value, HomeUser user, WebRequest request) {
        requireControl(homeId, deviceId, user, request);
        Fluxzero.sendCommandAndWait(new SetFanSpeed(deviceId, value.percent()));
    }

    @HandlePost("/water") @ApiDoc(operationId = "startWatering")
    void startWatering(@PathParam HomeId homeId, @PathParam DeviceId deviceId, HomeUser user, WebRequest request) {
        requireControl(homeId, deviceId, user, request);
        Fluxzero.sendCommandAndWait(new StartWatering(deviceId));
    }

    @HandlePost("/stop-watering") @ApiDoc(operationId = "stopWatering")
    void stopWatering(@PathParam HomeId homeId, @PathParam DeviceId deviceId, HomeUser user, WebRequest request) {
        requireControl(homeId, deviceId, user, request);
        Fluxzero.sendCommandAndWait(new StopWatering(deviceId));
    }

    @HandlePost("/charge") @ApiDoc(operationId = "enableCharging")
    void enableCharging(@PathParam HomeId homeId, @PathParam DeviceId deviceId, HomeUser user, WebRequest request) {
        requireControl(homeId, deviceId, user, request);
        Fluxzero.sendCommandAndWait(new EnableCharging(deviceId));
    }

    @HandlePost("/pause-charging") @ApiDoc(operationId = "pauseCharging")
    void pauseCharging(@PathParam HomeId homeId, @PathParam DeviceId deviceId, HomeUser user, WebRequest request) {
        requireControl(homeId, deviceId, user, request);
        Fluxzero.sendCommandAndWait(new PauseCharging(deviceId));
    }

    private static void requireControl(HomeId homeId, DeviceId deviceId, HomeUser user, WebRequest request) {
        BrowserRequests.requireSameOrigin(request);
        if (HomeAccess.require(homeId, user, HomePermission.CONTROL).find(deviceId, Device.class).isEmpty()) {
            throw new UnauthorizedException("Choose a device in this home.");
        }
    }
    public record Percent(@ApiDoc(required = true) @NotNull @Min(0) @Max(100) Integer percent) {}
    public record Color(@ApiDoc(required = true) @NotNull @Min(0) @Max(359) Integer hue,
                        @ApiDoc(required = true) @NotNull @Min(0) @Max(100) Integer saturation) {}
    public record Temperature(@ApiDoc(required = true) @NotNull @DecimalMin("5") @DecimalMax("35") BigDecimal celsius) {}
    public record Media(@ApiDoc(required = true) @NotBlank String media) {}
}
