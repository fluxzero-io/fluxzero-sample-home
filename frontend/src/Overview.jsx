import {
  CalendarClock,
  ChevronRight,
  DoorClosed,
  Leaf,
  Thermometer,
  Wifi,
} from "lucide-react";
import { dateIn, homeClimate, roomIds } from "./home.js";
import { Empty } from "./ui.jsx";

function temperature(views) {
  return views.find(
    (v) =>
      v.status?.availability === "ONLINE" &&
      v.status.readings?.TEMPERATURE != null,
  );
}

export function HomeSummary({ data, nextRoutine, navigate, openConnections }) {
  const linked = data.devices.filter((v) => v.delivery).length;
  const climate = homeClimate(data.devices);
  const climateRoom = data.spaces.find((space) => space.id === climate.spaceId);
  return (
    <div className="home-summary">
      <button className="summary-item" onClick={openConnections}>
        <Wifi size={21} />
        <span>
          <small>Device connections</small>
          <strong>
            {linked} of {data.devices.length} linked
          </strong>
        </span>
        <ChevronRight size={17} />
      </button>
      <div className="summary-item">
        <Thermometer size={21} />
        <span>
          <small>{climateRoom?.details.name || "Temperature"}</small>
          <span className="climate-values">
            <span>
              <small>Set</small>
              <strong>{climate.set ?? "—"}°C</strong>
            </span>
            <span>
              <small>Measured</small>
              <strong>{climate.measured ?? "—"}°C</strong>
            </span>
          </span>
        </span>
      </div>
      <button className="summary-item" onClick={() => navigate("routines")}>
        <CalendarClock size={21} />
        <span>
          <small>Next routine</small>
          <strong>
            {nextRoutine ? nextRoutine.details.name : "None scheduled"}
          </strong>
          {nextRoutine && (
            <small>{dateIn(nextRoutine.nextRun, data.home.timeZone)}</small>
          )}
        </span>
        <ChevronRight size={17} />
      </button>
    </div>
  );
}

export function RoomsOverview({ rooms, data, navigate }) {
  if (!rooms.length)
    return (
      <Empty icon={DoorClosed} title="No rooms yet">
        Rooms appear here when the home layout is added.
      </Empty>
    );
  return (
    <div className="rooms-grid">
      {rooms.map((room) => {
        const ids = roomIds(data.spaces, room.id);
        const devices = data.devices.filter((v) => ids.has(v.device.spaceId));
        const reading = temperature(devices);
        const hasThermometer = devices.some((v) =>
          v.device.measurements.includes("TEMPERATURE"),
        );
        const linked = devices.filter((v) => v.delivery).length;
        const issues = devices.filter(
          (v) =>
            v.delivery?.problem ||
            (v.status && v.status.availability !== "ONLINE"),
        ).length;
        const Icon = room.details.kind === "OUTDOOR" ? Leaf : DoorClosed;
        return (
          <button
            className="room-card"
            key={room.id}
            onClick={() => navigate("rooms", room.id)}
          >
            <span className="room-card-top">
              <span className="round-icon sage">
                <Icon size={23} />
              </span>
              <ChevronRight size={19} />
            </span>
            <h2>{room.details.name}</h2>
            <span className="room-device-count">
              {devices.length} {devices.length === 1 ? "device" : "devices"}
            </span>
            <span className="room-reading">
              {reading
                ? `${reading.status.readings.TEMPERATURE}°C`
                : hasThermometer
                  ? "Temperature unknown"
                  : devices
                      .slice(0, 2)
                      .map((v) => v.device.details.name)
                      .join(" · ") || "No devices yet"}
            </span>
            <span className={`room-card-foot ${issues ? "problem" : ""}`}>
              {issues
                ? `${issues} ${issues === 1 ? "device needs" : "devices need"} attention`
                : `${linked} of ${devices.length} linked`}
            </span>
          </button>
        );
      })}
    </div>
  );
}
