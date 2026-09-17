package io.fluxzero.home.household;

import io.fluxzero.common.api.Metadata;
import io.fluxzero.common.serialization.JsonUtils;
import io.fluxzero.home.access.AppAuthEndpoint;
import io.fluxzero.home.access.BrowserSessions;
import io.fluxzero.home.access.HomeUserProvider;
import io.fluxzero.home.access.api.AccountId;
import io.fluxzero.home.access.api.GrantHomeAccess;
import io.fluxzero.home.access.api.RevokeHomeAccess;
import io.fluxzero.home.access.api.model.AccountDetails;
import io.fluxzero.home.access.api.model.HomePermission;
import io.fluxzero.home.access.api.model.HomeUser;
import io.fluxzero.home.automation.RoutineEndpoint;
import io.fluxzero.home.automation.RoutineSchedules;
import io.fluxzero.home.automation.api.RoutineId;
import io.fluxzero.home.devices.DeviceEndpoint;
import io.fluxzero.home.devices.api.AddDevice;
import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.Charging;
import io.fluxzero.home.devices.api.model.DeviceDetails;
import io.fluxzero.home.devices.api.model.DeviceSetting;
import io.fluxzero.home.devices.api.model.DoorLock;
import io.fluxzero.home.devices.api.model.FanSpeed;
import io.fluxzero.home.devices.api.model.Irrigation;
import io.fluxzero.home.devices.api.model.LightColor;
import io.fluxzero.home.devices.api.model.LightLevel;
import io.fluxzero.home.devices.api.model.Opening;
import io.fluxzero.home.devices.api.model.Playback;
import io.fluxzero.home.devices.api.model.Power;
import io.fluxzero.home.devices.api.model.RoomTemperature;
import io.fluxzero.home.devices.api.model.Volume;
import io.fluxzero.home.household.api.AddSpace;
import io.fluxzero.home.household.api.ChangeHomeMode;
import io.fluxzero.home.household.api.CreateHome;
import io.fluxzero.home.household.api.HomeId;
import io.fluxzero.home.household.api.SpaceId;
import io.fluxzero.home.household.api.model.HomeDetails;
import io.fluxzero.home.household.api.model.HomeMode;
import io.fluxzero.home.household.api.model.HomeOverview;
import io.fluxzero.home.household.api.model.Space;
import io.fluxzero.home.household.api.model.SpaceDetails;
import io.fluxzero.home.household.api.model.SpaceKind;
import io.fluxzero.home.scenes.SceneEndpoint;
import io.fluxzero.home.scenes.api.SceneId;
import io.fluxzero.idp.testsupport.localstub.FluxzeroIdpStub;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.configuration.DefaultFluxzero;
import io.fluxzero.sdk.test.TestFixture;
import io.fluxzero.sdk.tracking.handling.authentication.DelegatingUserProvider;
import io.fluxzero.sdk.tracking.handling.authentication.UnauthenticatedException;
import io.fluxzero.sdk.tracking.handling.authentication.UnauthorizedException;
import io.fluxzero.sdk.tracking.handling.authentication.User;
import io.fluxzero.sdk.tracking.handling.validation.ValidationException;
import io.fluxzero.sdk.web.WebRequest;
import io.fluxzero.sdk.web.WebResponse;
import java.math.BigDecimal;
import java.net.HttpCookie;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static io.fluxzero.home.HouseExample.*;
import static org.junit.jupiter.api.Assertions.*;

class HomeWebTest {
    static final HomeUser OWNER = new HomeUser("alex");
    static final HomeUser VIEWER = new HomeUser("sam");
    static final String BASE = "http://localhost:8080";
    static final String HOME_URL = "/api/homes/" + HOME.getId();
    static final DeviceId MULTI = new DeviceId("multi-device");
    TestFixture fixture;

    TestFixture home() {
        var provider = new DelegatingUserProvider(new HomeUserProvider()) {
            @Override public User getActiveUser() { return User.getCurrent(); }
            @Override public User getSystemUser() { return null; }
        };
        fixture = populate(TestFixture.create(DefaultFluxzero.builder().registerUserProvider(provider),
                AppAuthEndpoint.class, HomeEndpoint.class, DeviceEndpoint.class, SceneEndpoint.class,
                RoutineEndpoint.class, SpaceEndpoint.class, HomeSocket.class, RoutineSchedules.class, FluxzeroIdpStub.class))
                .withProperty("fluxzero.auth.external-base-url", BASE)
                .withProperty("fluxzero.auth.oidc.issuer", BASE)
                .withProperty("fluxzero.auth.oidc.client-id", "local-auth-app")
                .withProperty("fluxzero.auth.oidc.redirect-uri", BASE + "/app/callback")
                .withProperty("fluxzero.auth.oidc.resource-audience", BASE + "/api")
                .withProperty("fluxzero.auth.oidc.login-state-secret", "home-test-shared-secret-at-least-32-characters")
                .givenCommandsByUser(HomeUser.SYSTEM,
                        new GrantHomeAccess(new AccountId(OWNER.id()), new AccountDetails("Alex"), HOME, HomePermission.MANAGE),
                        new GrantHomeAccess(new AccountId(VIEWER.id()), new AccountDetails("Sam"), HOME, HomePermission.VIEW))
                .givenCommands(evening());
        return fixture;
    }

    @AfterEach void close() {
        if (fixture != null) fixture.getFluxzero().close();
        FluxzeroIdpStub.reset();
    }

    static WebRequest request(String method, String path, Object body) {
        return WebRequest.builder().method(method).url(path).payload(body)
                .header("X-Home-Request", "1").header("Origin", BASE).build();
    }

    String session(HomeUser user) {
        return fixture.whenApplying(f -> BrowserSessions.create(user.id(), Fluxzero.currentTime().plusSeconds(3600)))
                .getResult(String.class);
    }

    WebRequest browser(String method, String path, Object body, String token) {
        return request(method, path, body).toBuilder().header("Cookie", "home_session=" + token).build();
    }

    @Test void cookieControlsTheHouseAndKeepsReportsSeparate() {
        home();
        String token = session(OWNER);
        fixture.givenWebRequest(browser("POST", HOME_URL + "/devices/" + LIGHT.getId() + "/brightness", Map.of("percent", 42), token))
                .whenWebRequest(browser("GET", HOME_URL, null, token))
                .expectWebResult(r -> {
                    assertEquals(200, r.getStatus());
                    HomeOverview view = r.getPayloadAs(HomeOverview.class);
                    assertEquals(HomePermission.MANAGE, view.permission());
                    var light = view.devices().stream().filter(d -> d.device().deviceId().equals(LIGHT)).findFirst().orElseThrow();
                    assertEquals(new LightLevel(42), light.device().pendingSettings().get(Capability.LIGHT_LEVEL));
                    assertNull(light.status());
                    return true;
                });
    }

    @Test void missingAndForgedCookiesCannotReadTheHome() {
        home().whenWebRequest(request("GET", HOME_URL, null)).expectExceptionalResult(UnauthenticatedException.class);
        fixture.whenWebRequest(browser("GET", HOME_URL, null, "x".repeat(43))).expectExceptionalResult(UnauthenticatedException.class);
    }

    @Test void viewerCanReadButCannotControl() {
        home();
        String token = session(VIEWER);
        fixture.whenWebRequest(browser("GET", HOME_URL, null, token)).expectWebResult(r -> r.getStatus() == 200);
        fixture.whenWebRequest(browser("POST", HOME_URL + "/devices/" + LIGHT.getId() + "/on", null, token))
                .expectExceptionalResult(UnauthorizedException.class).expectNoCommands();
    }

    @Test void anotherHouseAndCrossOriginMutationsAreRejected() {
        home();
        String token = session(OWNER);
        fixture.whenWebRequest(browser("GET", "/api/homes/other", null, token)).expectExceptionalResult(UnauthorizedException.class);
        fixture.whenWebRequest(WebRequest.post(HOME_URL + "/devices/" + LIGHT.getId() + "/on")
                        .header("Cookie", "home_session=" + token).header("Origin", "https://other.example").build())
                .expectExceptionalResult(UnauthorizedException.class).expectNoCommands();
    }

    @Test void localLoginAcquiresTheCookieUsedForControlAndLogoutRevokesIt() {
        // The IDP stub signs with wall-clock time, so this acquisition test uses that clock too.
        home().atFixedTime(Instant.now());
        var cookies = new LinkedHashMap<String, String>();
        WebResponse login = exchange(WebRequest.get("/app/login").build(), cookies);
        assertEquals(303, login.getStatus(), "start login");
        WebResponse authorize = exchange(WebRequest.get(login.getHeader("Location")).build(), cookies);
        assertEquals(302, authorize.getStatus(), "authorize");
        WebResponse signedIn = exchange(WebRequest.post(authorize.getHeader("Location"))
                .header("Content-Type", "application/x-www-form-urlencoded").payload("username=alex").build(), cookies);
        assertEquals(302, signedIn.getStatus(), "local identity");
        String callback = URI.create(signedIn.getHeader("Location")).getRawPath() + "?"
                + URI.create(signedIn.getHeader("Location")).getRawQuery();
        WebResponse completed = exchange(WebRequest.get(callback).build(), cookies);
        assertEquals("/", completed.getHeader("Location"), "callback accepted");
        assertTrue(cookies.containsKey("home_session"), "session issued");
        fixture.whenWebRequest(browser("PUT", HOME_URL + "/mode", Map.of("mode", "AWAY"), cookies.get("home_session")))
                .expectWebResult(r -> r.getStatus() < 300)
                .expectThat(f -> assertEquals(HomeMode.AWAY, Fluxzero.loadModel(HOME).get().mode()));
        String token = cookies.get("home_session");
        fixture.givenWebRequest(browser("POST", "/app/logout", null, token))
                .whenWebRequest(browser("GET", HOME_URL, null, token)).expectExceptionalResult(UnauthenticatedException.class);
    }


    static Stream<Arguments> deviceActions() {
        return Stream.of(
                Arguments.of("on", null, new Power(true)),
                Arguments.of("off", null, new Power(false)),
                Arguments.of("brightness", Map.of("percent", 37), new LightLevel(37)),
                Arguments.of("color", Map.of("hue", 240, "saturation", 80), new LightColor(240, 80)),
                Arguments.of("temperature", Map.of("celsius", 22), new RoomTemperature(new BigDecimal("22"))),
                Arguments.of("opening", Map.of("percent", 63), new Opening(63)),
                Arguments.of("lock", null, new DoorLock(true)),
                Arguments.of("unlock", null, new DoorLock(false)),
                Arguments.of("play", Map.of("media", "Evening radio"), new Playback(true, "Evening radio")),
                Arguments.of("stop", null, new Playback(false, null)),
                Arguments.of("volume", Map.of("percent", 26), new Volume(26)),
                Arguments.of("fan-speed", Map.of("percent", 52), new FanSpeed(52)),
                Arguments.of("water", null, new Irrigation(true)),
                Arguments.of("stop-watering", null, new Irrigation(false)),
                Arguments.of("charge", null, new Charging(true)),
                Arguments.of("pause-charging", null, new Charging(false)));
    }

    @ParameterizedTest @MethodSource("deviceActions")
    void everyControlRouteBindsItsSetting(String route, Object body, DeviceSetting expected) {
        home().givenCommands(new AddDevice(MULTI, LIVING, new DeviceDetails("All controls"), null,
                Set.of(Capability.values()), Set.of()));
        fixture.whenWebRequestByUser(OWNER.id(), request("POST", HOME_URL + "/devices/" + MULTI.getId() + "/" + route, body))
                .expectWebResult(r -> r.getStatus() < 300)
                .expectThat(f -> assertEquals(expected, Fluxzero.loadModel(MULTI).get().pendingSettings().get(expected.capability())));
    }

    @Test void aManagerCanCreateARoomInAnExistingFloor() {
        home();
        var details = new SpaceDetails("Study", SpaceKind.ROOM);
        fixture.whenWebRequest(browser("POST", HOME_URL + "/spaces",
                        new SpaceEndpoint.Definition(details, FLOOR), session(OWNER)))
                .expectWebResult(response -> {
                    assertEquals(201, response.getStatus());
                    SpaceEndpoint.CreatedSpace created = response.getPayloadAs(SpaceEndpoint.CreatedSpace.class);
                    var id = created.spaceId();
                    Space space = Fluxzero.loadModel(id).get();
                    assertEquals(FLOOR, space.parentId());
                    assertEquals(details, space.details());
                    return true;
                });
    }

    @Test void aRoomCanBeCreatedDirectlyInTheHome() {
        home().whenWebRequestByUser(OWNER, request("POST", HOME_URL + "/spaces",
                        Map.of("details", Map.of("name", "Study", "kind", "ROOM"))))
                .expectWebResult(response -> {
                    SpaceEndpoint.CreatedSpace created = response.getPayloadAs(SpaceEndpoint.CreatedSpace.class);
                    var id = created.spaceId();
                    assertEquals(HOME, Fluxzero.loadModel(id).get().parentId());
                    return response.getStatus() == 201;
                });
    }

    @ParameterizedTest @ValueSource(strings = {"VIEW", "CONTROL"})
    void creatingRoomsRequiresManagementAccess(String permission) {
        home().givenCommandsByUser(HomeUser.SYSTEM, new GrantHomeAccess(new AccountId(VIEWER.id()),
                        new AccountDetails("Sam"), HOME, HomePermission.valueOf(permission)))
                .whenWebRequestByUser(VIEWER, request("POST", HOME_URL + "/spaces",
                        new SpaceEndpoint.Definition(new SpaceDetails("Study", SpaceKind.ROOM), null)))
                .expectExceptionalResult(UnauthorizedException.class).expectNoCommands();
    }

    @Test void roomCreationRequiresAnAuthenticatedSameOriginRequest() {
        home();
        var body = new SpaceEndpoint.Definition(new SpaceDetails("Study", SpaceKind.ROOM), null);
        fixture.whenWebRequest(request("POST", HOME_URL + "/spaces", body))
                .expectExceptionalResult(UnauthenticatedException.class).expectNoCommands();
        fixture.whenWebRequest(browser("POST", HOME_URL + "/spaces", body, "x".repeat(43)))
                .expectExceptionalResult(UnauthenticatedException.class).expectNoCommands();
        fixture.whenWebRequest(WebRequest.post(HOME_URL + "/spaces").payload(body)
                        .header("Cookie", "home_session=" + session(OWNER))
                        .header("X-Home-Request", "1").header("Origin", "https://other.example").build())
                .expectExceptionalResult(UnauthorizedException.class).expectNoCommands();
    }

    @Test void roomCreationRejectsAnotherHomesLocation() {
        var other = new HomeId("other-home");
        var otherRoom = new SpaceId("other-room");
        home().givenCommands(new CreateHome(other, new HomeDetails("Other"), AMSTERDAM),
                        new AddSpace(otherRoom, other, new SpaceDetails("Study", SpaceKind.ROOM)))
                .whenWebRequestByUser(OWNER, request("POST", HOME_URL + "/spaces",
                        new SpaceEndpoint.Definition(new SpaceDetails("Study", SpaceKind.ROOM), otherRoom)))
                .expectExceptionalResult(UnauthorizedException.class).expectNoCommands();
    }

    @ParameterizedTest @ValueSource(strings = {
            "{}", "{\"details\":null}", "{\"details\":{\"name\":\" \",\"kind\":\"ROOM\"}}",
            "{\"details\":{\"name\":\"Study\"}}"})
    void roomCreationValidatesItsDetails(String json) {
        home().whenWebRequestByUser(OWNER, request("POST", HOME_URL + "/spaces", json))
                .expectExceptionalResult(ValidationException.class).expectNoCommands();
    }

    @Test void scenesCanBeDefinedActivatedAndRemoved() {
        home();
        String path = HOME_URL + "/scenes/reading";
        var body = Map.of("details", Map.of("name", "Reading"), "actions", List.of(Map.of(
                "kind", "dimLights", "target", Map.of("kind", "device", "deviceId", LIGHT.getId()),
                "brightness", Map.of("kind", "lightLevel", "percent", 65))));
        fixture.givenWebRequestByUser(OWNER, request("PUT", path, body))
                .whenWebRequestByUser(OWNER, request("POST", path + "/activate", null))
                .expectWebResult(r -> r.getStatus() < 300)
                .expectThat(f -> assertEquals(new LightLevel(65), Fluxzero.loadModel(LIGHT).get().pendingSettings().get(Capability.LIGHT_LEVEL)));
        fixture.whenWebRequestByUser(OWNER, request("DELETE", path, null)).expectWebResult(r -> r.getStatus() < 300)
                .expectThat(f -> assertTrue(Fluxzero.loadModel(new SceneId("reading")).isEmpty()));
    }

    @Test void routinesCanBePlannedPausedResumedAndRemovedWithTheirSchedule() {
        home();
        String path = HOME_URL + "/routines/bedtime";
        var body = Map.of("details", Map.of("name", "Bedtime"), "sceneId", EVENING.getId(),
                "timing", Map.of("kind", "weekly", "days", List.of("MONDAY", "FRIDAY"), "time", "22:00:00"));
        fixture.whenWebRequestByUser(OWNER, request("PUT", path, body))
                .expectWebResult(r -> r.getStatus() < 300)
                .expectThat(f -> assertEquals("Bedtime", Fluxzero.loadModel(BEDTIME).get().details().name()))
                .expectSchedule(s -> s.getScheduleId().equals(RoutineSchedules.scheduleId(BEDTIME).toString()));
        fixture.whenWebRequestByUser(OWNER, request("POST", path + "/pause", null)).expectWebResult(r -> r.getStatus() < 300)
                .expectThat(f -> assertFalse(Fluxzero.loadModel(BEDTIME).get().enabled())).expectNoSchedules();
        fixture.whenWebRequestByUser(OWNER, request("POST", path + "/resume", null)).expectWebResult(r -> r.getStatus() < 300)
                .expectThat(f -> assertTrue(Fluxzero.loadModel(BEDTIME).get().enabled()));
        fixture.whenWebRequestByUser(OWNER, request("DELETE", path, null)).expectWebResult(r -> r.getStatus() < 300)
                .expectThat(f -> assertTrue(Fluxzero.loadModel(BEDTIME).isEmpty())).expectNoSchedules();
    }

    @Test void malformedSettingAndNestedRoutineInputFailBeforeChangingState() {
        home().whenWebRequestByUser(OWNER, request("POST", HOME_URL + "/devices/" + LIGHT.getId() + "/brightness", Map.of("percent", 101)))
                .expectExceptionalResult(ValidationException.class);
        fixture.whenWebRequestByUser(OWNER, request("POST", HOME_URL + "/devices/" + LIGHT.getId() + "/brightness", Map.of()))
                .expectExceptionalResult(ValidationException.class);
        fixture.whenWebRequestByUser(OWNER, request("PUT", HOME_URL + "/routines/invalid", Map.of(
                        "details", Map.of("name", "Empty week"), "sceneId", EVENING.getId(),
                        "timing", Map.of("kind", "weekly", "days", List.of(), "time", "08:00:00"))))
                .expectExceptionalResult(ValidationException.class)
                .expectThat(f -> assertTrue(Fluxzero.loadModel(new RoutineId("invalid")).isEmpty()));
    }

    @Test void devicesOutsideTheHouseAreNotControllable() {
        HomeId other = new HomeId("other");
        SpaceId space = new SpaceId("other-room");
        DeviceId device = new DeviceId("other-light");
        home().givenCommands(new CreateHome(other, new HomeDetails("Other"), AMSTERDAM),
                        new AddSpace(space, other, new SpaceDetails("Room", SpaceKind.ROOM)),
                        new AddDevice(device, space, new DeviceDetails("Light"), null, Set.of(Capability.POWER), Set.of()))
                .whenWebRequestByUser(OWNER, request("POST", HOME_URL + "/devices/" + device.getId() + "/on", null))
                .expectExceptionalResult(UnauthorizedException.class).expectNoCommands();
    }

    @Test void controllerCanActivateButCannotRedefineScenes() {
        HomeUser controller = new HomeUser("jo");
        home().givenCommandsByUser(HomeUser.SYSTEM, new GrantHomeAccess(new AccountId("jo"), new AccountDetails("Jo"), HOME, HomePermission.CONTROL))
                .whenWebRequestByUser(controller, request("POST", HOME_URL + "/scenes/" + EVENING.getId() + "/activate", null))
                .expectWebResult(r -> r.getStatus() < 300).expectThat(f -> assertEvening());
        fixture.whenWebRequestByUser(controller, request("DELETE", HOME_URL + "/scenes/" + EVENING.getId(), null))
                .expectExceptionalResult(UnauthorizedException.class);
    }

    WebRequest socket(String method, String id, String token) {
        return browser(method, HOME_URL + "/live", null, token).toBuilder().metadata(Metadata.of("sessionId", id)).build();
    }

    @Test void liveViewStartsWithACompleteSnapshot() {
        home();
        String token = session(OWNER);
        fixture.whenWebRequest(socket("WS_OPEN", "first", token))
                .expectWebResponse(r -> "first".equals(r.getMetadata().get("sessionId"))
                        && r.getPayload() instanceof HomeOverview h && h.devices().size() == 3 && h.scenes().size() == 1);
    }

    @Test void twoOpenViewersBothReceiveCommittedChanges() {
        home();
        String owner = session(OWNER), viewer = session(VIEWER);
        fixture.givenWebRequest(socket("WS_OPEN", "owner", owner))
                .givenWebRequest(socket("WS_OPEN", "viewer", viewer))
                .whenCommand(new ChangeHomeMode(HOME, HomeMode.AWAY))
                .expectWebResponse(r -> "owner".equals(r.getMetadata().get("sessionId"))
                        && r.getPayload() instanceof HomeOverview h && h.home().mode() == HomeMode.AWAY)
                .expectWebResponse(r -> "viewer".equals(r.getMetadata().get("sessionId"))
                        && r.getPayload() instanceof HomeOverview h && h.home().mode() == HomeMode.AWAY);
    }

    @Test void expiredSessionCannotReadOrKeepALiveView() {
        home();
        String token = fixture.whenApplying(f -> BrowserSessions.create(OWNER.id(), Fluxzero.currentTime().plusSeconds(5)))
                .getResult(String.class);
        fixture.givenWebRequest(socket("WS_OPEN", "expiry", token)).givenElapsedTime(Duration.ofSeconds(6))
                .whenCommand(new ChangeHomeMode(HOME, HomeMode.AWAY))
                .expectWebResponse(r -> "expiry".equals(r.getMetadata().get("sessionId"))
                        && "close".equals(r.getMetadata().get("function")) && "1008".equals(r.getPayload()));
        fixture.whenWebRequest(browser("GET", HOME_URL, null, token)).expectExceptionalResult(UnauthenticatedException.class);
    }

    @Test void revokedMembershipClosesTheLiveViewAndRejectsFurtherControl() {
        home();
        String token = session(OWNER);
        fixture.givenWebRequest(socket("WS_OPEN", "revoked", token))
                .givenCommandsByUser(HomeUser.SYSTEM, new RevokeHomeAccess(new AccountId(OWNER.id()), HOME))
                .whenWebRequest(socket("WS_MESSAGE", "revoked", token))
                .expectWebResponse(r -> "revoked".equals(r.getMetadata().get("sessionId"))
                        && "close".equals(r.getMetadata().get("function")) && "1008".equals(r.getPayload()));
        fixture.whenWebRequest(browser("POST", HOME_URL + "/devices/" + LIGHT.getId() + "/on", null, token))
                .expectExceptionalResult(UnauthorizedException.class)
                .expectThat(f -> assertNull(Fluxzero.loadModel(LIGHT).get().pendingSettings().get(Capability.POWER)));
    }

    @Test void invalidCallbackCannotCreateASession() {
        home().whenGet("/app/callback?code=invalid&state=wrong")
                .expectWebResult(r -> "/?signin=login".equals(r.getHeader("Location"))
                        && r.getCookies().stream().noneMatch(c -> c.getName().equals("home_session")));
    }

    @Test void discoveryPublishesTheCookieAndEveryControl() {
        home().whenGet("/api/openapi.json").expectWebResult(response -> {
            String payload = response.getPayloadAs(String.class);
            var document = JsonUtils.readTree(payload);
            var paths = document.path("paths");
            String base = "/api/homes/{homeId}";
            assertEquals("getHome", paths.path(base).path("get").path("operationId").asText());
            assertEquals("changeHomeMode", paths.path(base + "/mode").path("put").path("operationId").asText());
            assertEquals("addSpace", paths.path(base + "/spaces").path("post").path("operationId").asText());
            assertTrue(paths.path(base + "/spaces").path("post").path("responses").has("201"));
            deviceActions().forEach(row -> assertTrue(paths.path(base + "/devices/{deviceId}/" + row.get()[0]).has("post")));
            assertEquals("defineScene", paths.path(base + "/scenes/{sceneId}").path("put").path("operationId").asText());
            assertTrue(paths.path(base + "/scenes/{sceneId}").has("delete"));
            assertTrue(paths.path(base + "/scenes/{sceneId}/activate").has("post"));
            assertTrue(paths.path(base + "/routines/{routineId}").has("put"));
            assertTrue(paths.path(base + "/routines/{routineId}").has("delete"));
            assertTrue(paths.path(base + "/routines/{routineId}/pause").has("post"));
            assertTrue(paths.path(base + "/routines/{routineId}/resume").has("post"));
            var scheme = document.path("components").path("securitySchemes").path("homeSession");
            assertEquals("home_session", scheme.path("name").asText());
            assertEquals("cookie", scheme.path("in").asText());
            assertTrue(document.path("security").get(0).has("homeSession"));
            return response.getStatus() == 200;
        });
    }

    WebResponse exchange(WebRequest request, Map<String, String> cookies) {
        String header = String.join("; ", cookies.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toList());
        WebResponse response = fixture.whenWebRequest(request.toBuilder().header("Cookie", header).build())
                .mapWebResultMessage(r -> r).getResult(WebResponse.class);
        response.getHeaders().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("Set-Cookie"))
                .flatMap(e -> e.getValue().stream()).flatMap(c -> HttpCookie.parse(c).stream())
                .forEach(c -> { if (c.getMaxAge() == 0) cookies.remove(c.getName()); else cookies.put(c.getName(), c.getValue()); });
        return response;
    }
}
