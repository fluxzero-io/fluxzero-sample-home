export const DAYS = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
];
export const UNITS = {
  TEMPERATURE: "°C",
  HUMIDITY: "%",
  ILLUMINANCE: "lx",
  POWER: "W",
  ENERGY: "kWh",
  BATTERY: "%",
  CARBON_DIOXIDE: "ppm",
};
export const setting = (settings, kind) =>
  settings?.find((s) => s.kind === kind);
export const titleCase = (value) =>
  (value || "")
    .toLowerCase()
    .replaceAll("_", " ")
    .replace(/(^|\s)\S/g, (s) => s.toUpperCase());
export const timeIn = (at, zone, options = {}) =>
  at
    ? new Intl.DateTimeFormat("en-GB", {
        timeZone: zone,
        hour: "2-digit",
        minute: "2-digit",
        ...options,
      }).format(new Date(at))
    : "—";
export const dateIn = (at, zone) =>
  timeIn(at, zone, { weekday: "short", day: "numeric", month: "short" });
export function roomIds(spaces, selected) {
  if (!selected) return new Set(spaces.map((s) => s.id));
  const result = new Set([selected]);
  let size;
  do {
    size = result.size;
    spaces.forEach((s) => {
      if (result.has(s.parentId?.[1])) result.add(s.id);
    });
  } while (result.size !== size);
  return result;
}
export function requestedOn(device) {
  return setting(device.desiredSettings, "power")?.on;
}
export function observation(view) {
  if (view.delivery?.problem)
    return { label: "Delivery issue", tone: "warning" };
  if (!view.status) return { label: "No report", tone: "muted" };
  if (view.status.availability !== "ONLINE")
    return { label: titleCase(view.status.availability), tone: "warning" };
  const desired = view.device.desiredSettings || [];
  const matches = desired.every((s) => {
    const report = setting(view.status.reportedSettings, s.kind);
    return report && Object.keys(s).every((k) => report[k] === s[k]);
  });
  return {
    label: !desired.length
      ? "Online"
      : matches
        ? "Confirmed"
        : "Awaiting confirmation",
    tone: matches ? "good" : "pending",
  };
}
export function timingLabel(timing, zone, includeTime = true) {
  if (timing.kind === "once")
    return includeTime ? dateIn(timing.at, zone) : dayIn(timing.at, zone);
  const days = DAYS.filter((d) => timing.days.includes(d));
  const group =
    days.length === 7
      ? "Every day"
      : days.join() === DAYS.slice(0, 5).join()
        ? "Weekdays"
        : days.map((d) => titleCase(d.slice(0, 3))).join(", ");
  return includeTime ? `${group} · ${timing.time.slice(0, 5)}` : group;
}
export const dayIn = (at, zone) =>
  new Intl.DateTimeFormat("en-GB", {
    timeZone: zone,
    weekday: "short",
    day: "numeric",
    month: "short",
  }).format(new Date(at));

export function sceneSummary(scene) {
  const summaries = scene.actions.map((action) => {
    switch (action.kind) {
      case "switchPower":
        return action.power.on ? "Power on" : "Power off";
      case "dimLights":
        return `Lights ${action.brightness.percent}%`;
      case "colorLights":
        return "Light color";
      case "setHeating":
        return `Heating ${action.temperature.celsius}°C`;
      case "positionCoverings":
        return `Shades ${action.opening.percent}%`;
      case "setLocks":
        return action.lock.locked ? "Lock doors" : "Unlock doors";
      case "setPlayback":
        return action.playback.playing ? "Play music" : "Stop music";
      case "adjustVolume":
        return `Volume ${action.volume.percent}%`;
      case "setVentilation":
        return `Fans ${action.speed.percent}%`;
      case "setWatering":
        return action.irrigation.watering ? "Watering on" : "Watering off";
      case "setCharging":
        return action.charging.enabled ? "Start charging" : "Pause charging";
      default:
        return titleCase(action.kind);
    }
  });
  return summaries.length > 2
    ? `${summaries.slice(0, 2).join(" · ")} · +${summaries.length - 2}`
    : summaries.join(" · ") || "No actions";
}
export const CAPABILITIES = {
  POWER: {
    label: "Power",
    kind: "switchPower",
    field: "power",
    value: { kind: "power", on: true },
  },
  LIGHT_LEVEL: {
    label: "Brightness",
    kind: "dimLights",
    field: "brightness",
    value: { kind: "lightLevel", percent: 50 },
  },
  LIGHT_COLOR: {
    label: "Color",
    kind: "colorLights",
    field: "color",
    value: { kind: "lightColor", hue: 35, saturation: 70 },
  },
  TEMPERATURE: {
    label: "Temperature",
    kind: "setHeating",
    field: "temperature",
    value: { kind: "temperature", celsius: 21 },
  },
  OPENING: {
    label: "Opening",
    kind: "positionCoverings",
    field: "opening",
    value: { kind: "opening", percent: 50 },
  },
  LOCK: {
    label: "Lock",
    kind: "setLocks",
    field: "lock",
    value: { kind: "lock", locked: true },
  },
  PLAYBACK: {
    label: "Media",
    kind: "setPlayback",
    field: "playback",
    value: { kind: "playback", playing: true, media: "" },
  },
  VOLUME: {
    label: "Volume",
    kind: "adjustVolume",
    field: "volume",
    value: { kind: "volume", percent: 30 },
  },
  FAN_SPEED: {
    label: "Fan speed",
    kind: "setVentilation",
    field: "speed",
    value: { kind: "fanSpeed", percent: 50 },
  },
  IRRIGATION: {
    label: "Watering",
    kind: "setWatering",
    field: "irrigation",
    value: { kind: "irrigation", watering: true },
  },
  CHARGING: {
    label: "Charging",
    kind: "setCharging",
    field: "charging",
    value: { kind: "charging", enabled: true },
  },
};
