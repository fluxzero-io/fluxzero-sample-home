package io.fluxzero.home.homeassistant;

import io.fluxzero.sdk.tracking.Consumer;
import io.fluxzero.sdk.web.HandleGet;
import io.fluxzero.sdk.web.HandlePost;
import io.fluxzero.sdk.web.WebResponse;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/** The remote installation in fixture scenarios; the real command/query handlers remain in the test flow. */
@Consumer(name = "home-assistant-http-stub", singleTracker = true)
public class HomeAssistantStub {
    public static final String BASE_URL = "https://home-assistant.example/ha";
    public static final String TOKEN = "example-token-not-a-secret";
    public volatile String snapshot = snapshot("off", 0, "68", "off");
    public volatile int readStatus = 200;
    public volatile int serviceStatus = 200;
    public final Queue<Integer> nextReadStatuses = new ConcurrentLinkedQueue<>();
    public final Queue<Integer> nextServiceStatuses = new ConcurrentLinkedQueue<>();

    @HandleGet(BASE_URL + "/api/states")
    WebResponse states() {
        var nextStatus = nextReadStatuses.poll();
        return WebResponse.builder().status(nextStatus == null ? readStatus : nextStatus)
                .contentType("application/json").payload(snapshot).build();
    }

    @HandlePost(BASE_URL + "/api/services/{domain}/{service}")
    WebResponse call() {
        var nextStatus = nextServiceStatuses.poll();
        return WebResponse.builder().status(nextStatus == null ? serviceStatus : nextStatus)
                .contentType("application/json").payload("[]").build();
    }

    public static String snapshot(String lightState, int brightness, String temperature, String motion) {
        return """
                [{"entity_id":"light.reading","state":"%s","attributes":{
                    "friendly_name":"Reading lamp","supported_color_modes":["brightness"],"brightness":%d}},
                 {"entity_id":"sensor.temperature","state":"%s","attributes":{
                    "friendly_name":"Temperature","device_class":"temperature","unit_of_measurement":"°F"}},
                 {"entity_id":"binary_sensor.motion","state":"%s","attributes":{
                    "friendly_name":"Motion","device_class":"motion"}}]
                """.formatted(lightState, brightness, temperature, motion);
    }
}
