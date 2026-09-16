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
  requestedOn,
  setting,
  titleCase,
} from "./home.js";
import { BinaryControl, IconButton, Range } from "./ui.jsx";

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
  const state = observation(view);
  const measurement = device.measurements.includes("TEMPERATURE")
    ? "TEMPERATURE"
    : device.measurements[0];
  const light = setting(device.desiredSettings, "lightLevel");
  const temp = setting(device.desiredSettings, "temperature");
  const opening = setting(device.desiredSettings, "opening");
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
        <IconButton
          label={`Details for ${device.details.name}`}
          onClick={onOpen}
        >
          <ChevronRight size={19} />
        </IconButton>
      </div>
      {device.capabilities.length > 0 && (
        <div className="requested-power">
          <span>Requested</span>
          {device.capabilities.includes("POWER") && (
            <BinaryControl
              label={`Requested power for ${device.details.name}`}
              value={requestedOn(device)}
              disabled={disabled || busy}
              choose={(on) => control(on ? "on" : "off").catch(() => {})}
            />
          )}
        </div>
      )}
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
          <span className="power-request">
            {requestedOn(device) == null
              ? "No power request"
              : requestedOn(device)
                ? "On requested"
                : "Off requested"}
          </span>
        ) : (
          <button className="device-more" onClick={onOpen}>
            More controls <ArrowRight size={15} />
          </button>
        )}
      </div>
      <div className="device-foot">
        <span className={`status-label ${state.tone}`}>
          <span />
          {state.label}
        </span>
        <span>
          {busy ? "Saving…" : view.delivery ? "Linked" : "Not linked"}
        </span>
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
  const [media, setMedia] = useState(
    setting(d.desiredSettings, "playback")?.media || "",
  );
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
        Controls change requested settings. Device reports are shown below.
      </p>
      <div className="detail-controls">
        {d.capabilities.includes("POWER") && (
          <div className="setting-row">
            <label>Requested power</label>
            <BinaryControl
              label="Requested power"
              value={requestedOn(d)}
              disabled={disabled}
              choose={(on) => send(on ? "on" : "off")}
            />
          </div>
        )}
        {d.capabilities.includes("LIGHT_LEVEL") && (
          <Range
            label="Brightness"
            value={setting(d.desiredSettings, "lightLevel")?.percent}
            disabled={disabled}
            commit={(v) => control("brightness", { percent: v })}
          />
        )}
        {d.capabilities.includes("TEMPERATURE") && (
          <div className="setting-row">
            <label>Requested temperature</label>
            <Temperature
              value={setting(d.desiredSettings, "temperature")?.celsius}
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
              value={setting(d.desiredSettings, "lightColor")?.hue}
              disabled={disabled}
              commit={(v) =>
                control("color", {
                  hue: v,
                  saturation:
                    setting(d.desiredSettings, "lightColor")?.saturation ?? 100,
                })
              }
            />
            <Range
              label="Saturation"
              value={setting(d.desiredSettings, "lightColor")?.saturation}
              disabled={disabled}
              commit={(v) =>
                control("color", {
                  hue: setting(d.desiredSettings, "lightColor")?.hue ?? 0,
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
                setting(
                  d.desiredSettings,
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
                const locked = setting(d.desiredSettings, "lock")?.locked;
                if (!locked || confirm("Unlock this door?"))
                  send(locked ? "unlock" : "lock");
              }}
            >
              {setting(d.desiredSettings, "lock")?.locked ? "Unlock" : "Lock"}
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
                onClick={() => send("play", { media })}
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
              label="Requested watering"
              value={setting(d.desiredSettings, "irrigation")?.watering}
              disabled={disabled}
              choose={(on) => send(on ? "water" : "stop-watering")}
            />
          </div>
        )}
        {d.capabilities.includes("CHARGING") && (
          <div className="setting-row">
            <label>Charging</label>
            <BinaryControl
              label="Requested charging"
              value={setting(d.desiredSettings, "charging")?.enabled}
              disabled={disabled}
              choose={(on) => send(on ? "charge" : "pause-charging")}
            />
          </div>
        )}
      </div>
      <h3 className="subheading">Requested & reported</h3>
      <div className="state-table">
        <div className="state-table-head">
          <span>Setting</span>
          <span>Requested</span>
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
                {formatSetting(setting(d.desiredSettings, kinds[c]), "Not set")}
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
