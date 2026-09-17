import test from "node:test";
import assert from "node:assert/strict";
import {
  observation,
  controlSetting,
  awaitingSync,
  homeClimate,
  roomIds,
  timingLabel,
  sceneSummary,
} from "./home.js";

test("a requested setting is never treated as a physical acknowledgement", () => {
  const device = { pendingSettings: [{ kind: "lightLevel", percent: 42 }] };
  assert.equal(observation({ device }).label, "No report");
  assert.equal(
    observation({
      device,
      status: { availability: "ONLINE", reportedSettings: [] },
    }).label,
    "Awaiting confirmation",
  );
  assert.equal(
    observation({
      device,
      status: {
        availability: "ONLINE",
        reportedSettings: device.pendingSettings,
      },
    }).label,
    "Awaiting confirmation",
  );
  assert.equal(
    observation({
      device,
      status: {
        availability: "OFFLINE",
        reportedSettings: device.pendingSettings,
      },
    }).label,
    "Offline",
  );
  assert.equal(
    observation({ device, delivery: { problem: "Unavailable" } }).label,
    "Delivery issue",
  );
});

test("a room filter includes nested spaces regardless of their order", () => {
  const spaces = [
    { id: "alcove", parentId: ["space", "living"] },
    { id: "kitchen", parentId: ["home", "house"] },
    { id: "living", parentId: ["space", "floor"] },
    { id: "floor", parentId: ["home", "house"] },
  ];
  assert.deepEqual([...roomIds(spaces, "floor")].sort(), [
    "alcove",
    "floor",
    "living",
  ]);
  assert.equal(roomIds(spaces, "").size, 4);
});

test("explicit requested power off takes priority over an earlier brightness", () => {
  assert.equal(
    controlSetting({
      device: {
        pendingSettings: [
          { kind: "power", on: false },
          { kind: "lightLevel", percent: 80 },
        ],
      },
    }, "power").on,
    false,
  );
});

test("weekly timing is shown in weekday order and one-off timing in the home timezone", () => {
  assert.equal(
    timingLabel(
      { kind: "weekly", days: ["FRIDAY", "MONDAY"], time: "20:30:00" },
      "Europe/Amsterdam",
    ),
    "Mon, Fri · 20:30",
  );
  assert.match(
    timingLabel(
      { kind: "once", at: "2026-09-16T18:00:00Z" },
      "Europe/Amsterdam",
    ),
    /20:00/,
  );
});

test("brightness alone does not invent a power request", () => {
  assert.equal(
    controlSetting({ device: { pendingSettings: [{ kind: "lightLevel", percent: 80 }] } }, "power"),
    undefined,
  );
  assert.equal(controlSetting({ device: { pendingSettings: [] } }, "power"), undefined);
  assert.equal(
    controlSetting({ device: { pendingSettings: [{ kind: "power", on: true }] } }, "power").on,
    true,
  );
});

test("an online device without a request is not called confirmed", () => {
  assert.equal(
    observation({
      device: { pendingSettings: [] },
      status: { availability: "ONLINE" },
    }).label,
    "Online",
  );
});

test("scene summaries describe their intentions, without claiming execution", () => {
  assert.equal(
    sceneSummary({
      actions: [
        { kind: "dimLights", brightness: { percent: 25 } },
        { kind: "setHeating", temperature: { celsius: 21 } },
      ],
    }),
    "Lights 25% · Heating 21°C",
  );
});

test("sync feedback waits for the backend to finish the request, not a service acknowledgement", () => {
  const device = { pendingSettings: [{ kind: "power", on: true }] };
  const linked = { device, delivery: {} };
  assert.equal(awaitingSync({ device }), false);
  assert.equal(awaitingSync(linked), true);
  assert.equal(
    awaitingSync({
      ...linked,
      status: { availability: "ONLINE", reportedSettings: [] },
    }),
    true,
  );
  assert.equal(
    awaitingSync({
      ...linked,
      status: {
        availability: "ONLINE",
        reportedSettings: device.pendingSettings,
      },
    }),
    true,
  );
  assert.equal(
    awaitingSync({
      ...linked,
      status: {
        availability: "OFFLINE",
        reportedSettings: device.pendingSettings,
      },
    }),
    true,
  );
  assert.equal(
    awaitingSync({ ...linked, delivery: { problem: "Unavailable" } }),
    false,
  );
  assert.equal(
    awaitingSync({ device: { pendingSettings: [] }, delivery: {} }),
    false,
  );
});

test("the climate summary pairs set and measured temperature in the same room", () => {
  const thermostat = {
    device: {
      spaceId: "living",
      capabilities: ["TEMPERATURE"],
      measurements: [],
      pendingSettings: [{ kind: "temperature", celsius: 21 }],
    },
  };
  const sensor = (spaceId, celsius, availability = "ONLINE") => ({
    device: { spaceId, capabilities: [], measurements: ["TEMPERATURE"] },
    status: { availability, readings: { TEMPERATURE: celsius } },
  });
  assert.deepEqual(
    homeClimate([sensor("bedroom", 18), thermostat, sensor("living", 20.5)]),
    { spaceId: "living", set: 21, measured: 20.5 },
  );
  assert.deepEqual(
    homeClimate([
      sensor("bedroom", 18),
      thermostat,
      sensor("living", 20.5, "OFFLINE"),
    ]),
    { spaceId: "living", set: 21, measured: undefined },
  );
  assert.deepEqual(homeClimate([sensor("living", 0)]), {
    spaceId: "living",
    set: undefined,
    measured: 0,
  });
  assert.deepEqual(homeClimate([]), {
    spaceId: undefined,
    set: undefined,
    measured: undefined,
  });
});
test("controls follow physical changes after a Home request has finished", () => {
  const view = {
    device: { pendingSettings: [] },
    delivery: {},
    status: {
      availability: "ONLINE",
      reportedSettings: [
        { kind: "power", on: false },
        { kind: "lightLevel", percent: 0 },
      ],
    },
  };
  assert.equal(controlSetting(view, "power").on, false);
  assert.equal(controlSetting(view, "lightLevel").percent, 0);
  assert.equal(observation(view).label, "Online");
  assert.equal(awaitingSync(view), false);
  view.device.pendingSettings = [{ kind: "lightLevel", percent: 40 }];
  assert.equal(controlSetting(view, "lightLevel").percent, 40);
  assert.equal(controlSetting(view, "power").on, false);
  assert.equal(awaitingSync(view), true);
});

test("observed brightness and color are usable before Home has sent any request", () => {
  const view = {
    device: { pendingSettings: [] },
    status: {
      availability: "ONLINE",
      reportedSettings: [
        { kind: "lightLevel", percent: 71 },
        { kind: "lightColor", hue: 210, saturation: 70 },
      ],
    },
  };
  assert.equal(controlSetting(view, "lightLevel").percent, 71);
  assert.deepEqual(controlSetting(view, "lightColor"), view.status.reportedSettings[1]);
  assert.equal(controlSetting(view, "opening"), undefined);
  assert.equal(controlSetting({ ...view, status: { ...view.status, availability: "OFFLINE" } }, "lightLevel"), undefined);
});

test("the climate summary uses the reported thermostat setpoint, not measured room temperature", () => {
  const view = {
    device: {
      spaceId: "living", capabilities: ["TEMPERATURE"],
      measurements: ["TEMPERATURE"], pendingSettings: [],
    },
    status: {
      availability: "ONLINE",
      reportedSettings: [{ kind: "temperature", celsius: 22 }],
      readings: { TEMPERATURE: 19 },
    },
  };
  assert.deepEqual(homeClimate([view]), { spaceId: "living", set: 22, measured: 19 });
});
