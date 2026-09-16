import test from "node:test";
import assert from "node:assert/strict";
import {
  observation,
  awaitingSync,
  homeClimate,
  roomIds,
  timingLabel,
  requestedOn,
  sceneSummary,
} from "./home.js";

test("a requested setting is never treated as a physical acknowledgement", () => {
  const device = { desiredSettings: [{ kind: "lightLevel", percent: 42 }] };
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
        reportedSettings: device.desiredSettings,
      },
    }).label,
    "Confirmed",
  );
  assert.equal(
    observation({
      device,
      status: {
        availability: "OFFLINE",
        reportedSettings: device.desiredSettings,
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
    requestedOn({
      desiredSettings: [
        { kind: "power", on: false },
        { kind: "lightLevel", percent: 80 },
      ],
    }),
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
    requestedOn({ desiredSettings: [{ kind: "lightLevel", percent: 80 }] }),
    undefined,
  );
  assert.equal(requestedOn({ desiredSettings: [] }), undefined);
  assert.equal(
    requestedOn({ desiredSettings: [{ kind: "power", on: true }] }),
    true,
  );
});

test("an online device without a request is not called confirmed", () => {
  assert.equal(
    observation({
      device: { desiredSettings: [] },
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

test("sync feedback waits for an online matching report, not a service acknowledgement", () => {
  const device = { desiredSettings: [{ kind: "power", on: true }] };
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
        reportedSettings: device.desiredSettings,
      },
    }),
    false,
  );
  assert.equal(
    awaitingSync({
      ...linked,
      status: {
        availability: "OFFLINE",
        reportedSettings: device.desiredSettings,
      },
    }),
    true,
  );
  assert.equal(
    awaitingSync({ ...linked, delivery: { problem: "Unavailable" } }),
    false,
  );
  assert.equal(
    awaitingSync({ device: { desiredSettings: [] }, delivery: {} }),
    false,
  );
});

test("the climate summary pairs set and measured temperature in the same room", () => {
  const thermostat = {
    device: {
      spaceId: "living",
      capabilities: ["TEMPERATURE"],
      measurements: [],
      desiredSettings: [{ kind: "temperature", celsius: 21 }],
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
