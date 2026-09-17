import { useState } from "react";
import {
  ArrowRight,
  Blinds,
  ChevronRight,
  DoorClosed,
  Fan,
  Lamp,
  Leaf,
  Lightbulb,
  LoaderCircle,
  Minus,
  Music2,
  Play,
  Plus,
  Power,
  Radio,
  Thermometer,
  Zap,
} from "lucide-react";
import {
  CAPABILITIES,
  UNITS,
  dateIn,
  observation,
  awaitingSync,
  controlSetting,
  setting,
  titleCase,
} from "./home.js";
import { BinaryControl, IconButton, Range, Toggle } from "./ui.jsx";

const icons = {
  POWER: Power,
  LIGHT_LEVEL: Lamp,
  LIGHT_COLOR: Lightbulb,
  TEMPERATURE: Thermometer,
  OPENING: Blinds,
  LOCK: DoorClosed,
  PLAYBACK: Music2,
  VOLUME: Music2,
  FAN_SPEED: Fan,
  IRRIGATION: Leaf,
  CHARGING: Zap,
};
const deviceIcon = (device) =>
  icons[
    device.capabilities.find((c) => c !== "POWER") || device.capabilities[0]
  ] || Radio;
export function DeviceCard({
  view,
  roomName,
  busy,
  disabled,
  onOpen,
  control,
}) {
  const { device, status } = view;
  const Icon = deviceIcon(device);
  const reportedOn =
    status?.availability === "ONLINE" &&
    setting(status.reportedSettings, "power")?.on === true;
  const power = controlSetting(view, "power")?.on;
  const state = observation(view);
  const syncing = busy || awaitingSync(view);
  const measurement = device.measurements.includes("TEMPERATURE")
    ? "TEMPERATURE"
    : device.measurements[0];
  const light = controlSetting(view, "lightLevel");
  const temp = controlSetting(view, "temperature");
  const opening = controlSetting(view, "opening");
  return (
    <article className={`device-card ${reportedOn ? "is-on" : ""}`}>
      <div className="device-top">
        <button
          className={`device-symbol ${reportedOn ? "lit" : ""}`}
          aria-label={`Open ${device.details.name}`}
          onClick={onOpen}
        >
          <Icon size={25} strokeWidth={1.65} />
        </button>
        <button className="device-title" onClick={onOpen}>
          <span>{roomName}</span>
          <h3>{device.details.name}</h3>
        </button>
        {device.capabilities.includes("POWER") ? (
          <Toggle
            label={`Power for ${device.details.name}`}
            checked={power}
            disabled={disabled || busy}
            onClick={() => control(power ? "off" : "on").catch(() => {})}
          />
        ) : (
          <IconButton
            label={`Details for ${device.details.name}`}
            onClick={onOpen}
          >
            <ChevronRight size={19} />
          </IconButton>
        )}
      </div>
      <div className="device-control">
        {device.capabilities.includes("LIGHT_LEVEL") ? (
          <Range
            label="Brightness"
            value={light?.percent}
            disabled={disabled || busy}
            commit={(v) => control("brightness", { percent: v })}
          />
        ) : device.capabilities.includes("TEMPERATURE") ? (
          <Temperature
            value={temp?.celsius}
            disabled={disabled || busy}
            commit={(v) => control("temperature", { celsius: v })}
          />
        ) : device.capabilities.includes("OPENING") ? (
          <Range
            label="Opening"
            value={opening?.percent}
            disabled={disabled || busy}
            commit={(v) => control("opening", { percent: v })}
          />
        ) : device.measurements.length ? (
          <div className="sensor-value">
            {status?.readings?.[measurement] ?? "—"}
            <span>{UNITS[measurement] || ""}</span>
            <small>{status ? "Last reported" : "Awaiting first reading"}</small>
          </div>
        ) : device.capabilities.length === 1 &&
          device.capabilities[0] === "POWER" ? (
          <span className="power-state">
            {power == null ? "Power unknown" : power ? "On" : "Off"}
          </span>
        ) : (
          <button className="device-more" onClick={onOpen}>
            More controls <ArrowRight size={15} />
          </button>
        )}
      </div>
      <div className="device-foot">
        {state.tone !== "pending" ? (
          <span className={`status-label ${state.tone}`}>
            <span />
            {state.label}
          </span>
        ) : (
          <span />
        )}
        {syncing ? (
          <span
            className="sync-indicator"
            role="status"
            title={
              view.delivery
                ? "Waiting for the device to confirm this setting"
                : "Saving your setting"
            }
          >
            <LoaderCircle size={14} /> {view.delivery ? "Syncing" : "Saving"}
          </span>
        ) : (
          <span>{view.delivery ? "Linked" : "Not linked"}</span>
        )}
      </div>
    </article>
  );
}

export function Temperature({ value, disabled, commit }) {
  const temp = value ?? 21;
  return (
    <div className="temperature">
      <IconButton
        label="Lower temperature"
        disabled={disabled || temp <= 5}
        onClick={() => commit(Math.max(5, temp - 0.5)).catch(() => {})}
      >
        <Minus size={17} />
      </IconButton>
      <span>
        {value ?? "—"}
        <small>°C</small>
      </span>
      <IconButton
        label="Raise temperature"
        disabled={disabled || temp >= 35}
        onClick={() => commit(Math.min(35, temp + 0.5)).catch(() => {})}
      >
        <Plus size={17} />
      </IconButton>
    </div>
  );
}

export function DeviceDetail({ view, roomName, zone, disabled, control }) {
  const d = view.device;
  const state = observation(view);
  const [mediaDraft, setMedia] = useState(null);
  const media = mediaDraft ?? controlSetting(view, "playback")?.media ?? "";
  const send = (r, b) => control(r, b).catch(() => {});
  return (
    <div className="device-detail">
      <p className="detail-location">
        {roomName}
        <span className={`status-label ${state.tone}`}>
          <span />
          {state.label}
        </span>
      </p>
      {view.delivery?.problem && (
        <p className="form-error">{view.delivery.problem}</p>
      )}
      <p className="control-caption">
        Device changes appear here automatically.
      </p>
      <div className="detail-controls">
        {d.capabilities.includes("POWER") && (
          <div className="setting-row">
            <label>Power</label>
            <BinaryControl
              label="Power"
              value={controlSetting(view, "power")?.on}
              disabled={disabled}
              choose={(on) => send(on ? "on" : "off")}
            />
          </div>
        )}
        {d.capabilities.includes("LIGHT_LEVEL") && (
          <Range
            label="Brightness"
            value={controlSetting(view, "lightLevel")?.percent}
            disabled={disabled}
            commit={(v) => control("brightness", { percent: v })}
          />
        )}
        {d.capabilities.includes("TEMPERATURE") && (
          <div className="setting-row">
            <label>Temperature</label>
            <Temperature
              value={controlSetting(view, "temperature")?.celsius}
              disabled={disabled}
              commit={(v) => control("temperature", { celsius: v })}
            />
          </div>
        )}
        {d.capabilities.includes("LIGHT_COLOR") && (
          <>
            <Range
              label="Hue"
              unit="°"
              max={359}
              value={controlSetting(view, "lightColor")?.hue}
              disabled={disabled}
              commit={(v) =>
                control("color", {
                  hue: v,
                  saturation:
                    controlSetting(view, "lightColor")?.saturation ?? 100,
                })
              }
            />
            <Range
              label="Saturation"
              value={controlSetting(view, "lightColor")?.saturation}
              disabled={disabled}
              commit={(v) =>
                control("color", {
                  hue: controlSetting(view, "lightColor")?.hue ?? 0,
                  saturation: v,
                })
              }
            />
          </>
        )}
        {["OPENING", "VOLUME", "FAN_SPEED"]
          .filter((c) => d.capabilities.includes(c))
          .map((c) => (
            <Range
              key={c}
              label={CAPABILITIES[c].label}
              value={
                controlSetting(
                  view,
                  {
                    OPENING: "opening",
                    VOLUME: "volume",
                    FAN_SPEED: "fanSpeed",
                  }[c],
                )?.percent
              }
              disabled={disabled}
              commit={(v) =>
                control(
                  {
                    OPENING: "opening",
                    VOLUME: "volume",
                    FAN_SPEED: "fan-speed",
                  }[c],
                  { percent: v },
                )
              }
            />
          ))}
        {d.capabilities.includes("LOCK") && (
          <div className="setting-row">
            <label>Door lock</label>
            <button
              className="secondary"
              disabled={disabled}
              onClick={() => {
                const locked = controlSetting(view, "lock")?.locked;
                if (!locked || confirm("Unlock this door?"))
                  send(locked ? "unlock" : "lock");
              }}
            >
              {controlSetting(view, "lock")?.locked ? "Unlock" : "Lock"}
            </button>
          </div>
        )}
        {d.capabilities.includes("PLAYBACK") && (
          <div className="field">
            <label htmlFor="media-source">Media</label>
            <input
              id="media-source"
              value={media}
              onChange={(e) => setMedia(e.target.value)}
              disabled={disabled}
            />
            <div className="inline-actions">
              <button
                className="secondary"
                disabled={disabled || !media.trim()}
                onClick={async () => {
                  try {
                    await control("play", { media });
                    setMedia(null);
                  } catch {}
                }}
              >
                <Play size={15} />
                Play
              </button>
              <button
                className="secondary"
                disabled={disabled}
                onClick={() => send("stop")}
              >
                Stop
              </button>
            </div>
          </div>
        )}
        {d.capabilities.includes("IRRIGATION") && (
          <div className="setting-row">
            <label>Watering</label>
            <BinaryControl
              label="Watering"
              value={controlSetting(view, "irrigation")?.watering}
              disabled={disabled}
              choose={(on) => send(on ? "water" : "stop-watering")}
            />
          </div>
        )}
        {d.capabilities.includes("CHARGING") && (
          <div className="setting-row">
            <label>Charging</label>
            <BinaryControl
              label="Charging"
              value={controlSetting(view, "charging")?.enabled}
              disabled={disabled}
              choose={(on) => send(on ? "charge" : "pause-charging")}
            />
          </div>
        )}
      </div>
      <h3 className="subheading">Pending & reported</h3>
      <div className="state-table">
        <div className="state-table-head">
          <span>Setting</span>
          <span>Pending</span>
          <span>Reported</span>
        </div>
        {d.capabilities.map((c) => {
          const kinds = {
            POWER: "power",
            LIGHT_LEVEL: "lightLevel",
            LIGHT_COLOR: "lightColor",
            TEMPERATURE: "temperature",
            OPENING: "opening",
            LOCK: "lock",
            PLAYBACK: "playback",
            VOLUME: "volume",
            FAN_SPEED: "fanSpeed",
            IRRIGATION: "irrigation",
            CHARGING: "charging",
          };
          return (
            <div key={c}>
              <span>{CAPABILITIES[c]?.label || titleCase(c)}</span>
              <strong>
                {formatSetting(setting(d.pendingSettings, kinds[c]), "—")}
              </strong>
              <strong>
                {formatSetting(
                  setting(view.status?.reportedSettings, kinds[c]),
                )}
              </strong>
            </div>
          );
        })}
        {d.measurements.map((m) => (
          <div key={m}>
            <span>{titleCase(m)}</span>
            <span>—</span>
            <strong>
              {view.status?.readings?.[m] ?? "—"} {UNITS[m] || ""}
            </strong>
          </div>
        ))}
      </div>
      <p className="small muted">
        {view.status
          ? `Last report: ${dateIn(view.status.observedAt, zone)}`
          : "No device report received yet."}
      </p>
    </div>
  );
}

function formatSetting(s, missing = "Unknown") {
  if (!s) return missing;
  if ("percent" in s) return `${s.percent}%`;
  if ("celsius" in s) return `${s.celsius}°C`;
  if ("hue" in s) return `${s.hue}° / ${s.saturation}%`;
  if ("on" in s) return s.on ? "On" : "Off";
  if ("locked" in s) return s.locked ? "Locked" : "Unlocked";
  if ("playing" in s) return s.playing ? "Playing" : "Stopped";
  if ("watering" in s) return s.watering ? "Watering" : "Off";
  if ("enabled" in s) return s.enabled ? "Enabled" : "Paused";
  return "—";
}
