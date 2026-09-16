import { useCallback, useEffect, useRef, useState } from "react";
import {
  ArrowLeft,
  ArrowRight,
  ArrowUpRight,
  CalendarClock,
  Check,
  ChevronRight,
  CircleAlert,
  Clock3,
  DoorClosed,
  House,
  LayoutGrid,
  Menu,
  Moon,
  Pencil,
  Plus,
  Search,
  Sparkles,
  Sun,
  Wifi,
  X,
} from "lucide-react";
import { dayIn, roomIds, timeIn, timingLabel, titleCase } from "./home.js";
import {
  IconButton,
  Empty,
  Dialog,
  Toggle,
  SectionTitle,
  HomeDrawing,
  ProfileMenu,
} from "./ui.jsx";
import { api } from "./api.js";
import { DeviceCard, DeviceDetail } from "./Devices.jsx";
import { SceneCard, SceneEditor } from "./Scenes.jsx";
import { RoutineEditor } from "./Routines.jsx";
import { HomeSummary, RoomsOverview } from "./Overview.jsx";

const modes = {
  HOME: [House, "Home"],
  AWAY: [ArrowUpRight, "Away"],
  SLEEPING: [Moon, "Night"],
  VACATION: [Sun, "Vacation"],
};

export function App() {
  const [account, setAccount] = useState(null);
  const [authChecked, setAuthChecked] = useState(false);
  const [homeId, setHomeId] = useState("");
  const [data, setData] = useState(null);
  const [page, setPage] = useState("overview");
  const [room, setRoom] = useState("");
  const [search, setSearch] = useState("");
  const [dialog, setDialog] = useState(null);
  const [busy, setBusy] = useState(new Set());
  const [toast, setToast] = useState(null);
  const [error, setError] = useState("");
  const [navOpen, setNavOpen] = useState(false);
  const homeRef = useRef(homeId);
  homeRef.current = homeId;
  const refreshAccount = useCallback(async () => {
    try {
      const result = await api("/app/auth/session");
      setAccount(result.authenticated ? result : null);
      if (result.authenticated)
        setHomeId((current) =>
          result.homes.some((h) => h.home.id === current)
            ? current
            : result.homes[0]?.home.id || "",
        );
      else setHomeId("");
    } catch (e) {
      setError("Could not connect to Home. Try again.");
    } finally {
      setAuthChecked(true);
    }
  }, []);
  useEffect(() => {
    refreshAccount();
  }, [refreshAccount]);
  const reload = useCallback(async () => {
    if (!homeId) return;
    const result = await api(`/api/homes/${encodeURIComponent(homeId)}`);
    if (homeRef.current === homeId) {
      setData(result);
      setError("");
    }
  }, [homeId]);
  useEffect(() => {
    setData(null);
    setRoom("");
    setDialog(null);
    if (!homeId) return;
    const abort = new AbortController();
    api(`/api/homes/${encodeURIComponent(homeId)}`, { signal: abort.signal })
      .then((result) => {
        if (homeRef.current === homeId && !abort.signal.aborted) {
          setData(result);
          setError("");
        }
      })
      .catch((e) => {
        if (e.name !== "AbortError" && homeRef.current === homeId) {
          setError(e.message);
          if (e.status === 401) refreshAccount();
        }
      });
    return () => abort.abort();
  }, [homeId, refreshAccount]);
  useEffect(() => {
    if (!homeId) return;
    let socket,
      stopped = false,
      retry,
      heartbeat,
      attempt = 0;
    const connect = () => {
      socket = new WebSocket(
        `${location.protocol === "https:" ? "wss" : "ws"}://${location.host}/api/homes/${encodeURIComponent(homeId)}/live`,
      );
      socket.onopen = () => {
        attempt = 0;
        heartbeat = setInterval(() => {
          if (socket.readyState === WebSocket.OPEN) socket.send("refresh");
        }, 30000);
      };
      socket.onmessage = (event) => {
        try {
          const snapshot = JSON.parse(event.data);
          if (
            !stopped &&
            homeRef.current === homeId &&
            snapshot.home?.id === homeId
          ) {
            setData(snapshot);
            setError("");
          }
        } catch {
          setError("Could not refresh your home.");
        }
      };
      socket.onclose = (event) => {
        clearInterval(heartbeat);
        if (stopped) return;
        if (event.code === 1008) {
          refreshAccount();
        }
        retry = setTimeout(connect, Math.min(15000, 1000 * 2 ** attempt++));
      };
    };
    connect();
    const focus = () => {
      if (document.visibilityState === "visible") reload().catch(() => {});
    };
    document.addEventListener("visibilitychange", focus);
    return () => {
      stopped = true;
      clearTimeout(retry);
      clearInterval(heartbeat);
      socket?.close();
      document.removeEventListener("visibilitychange", focus);
    };
  }, [homeId, reload, refreshAccount]);
  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(null), 4500);
    return () => clearTimeout(timer);
  }, [toast]);
  async function act(key, path, body, method = "POST", success) {
    if (busy.has(key)) return;
    setBusy((prev) => new Set(prev).add(key));
    try {
      await api(`/api/homes/${encodeURIComponent(homeId)}${path}`, {
        method,
        body,
      });
      await reload();
      if (success) setToast({ text: success });
    } catch (e) {
      setToast({ text: e.message, error: true });
      if (e.status === 401) refreshAccount();
      throw e;
    } finally {
      setBusy((prev) => {
        const next = new Set(prev);
        next.delete(key);
        return next;
      });
    }
  }
  const control = (id, route, body) =>
    act(id, `/devices/${encodeURIComponent(id)}/${route}`, body);
  const activate = (scene) =>
    act(
      scene.sceneId,
      `/scenes/${encodeURIComponent(scene.sceneId)}/activate`,
      undefined,
      "POST",
      `${scene.details.name} requested`,
    ).catch(() => {});
  const navigate = (next, space = "") => {
    setPage(next);
    setRoom(space);
    setSearch("");
    setNavOpen(false);
    window.scrollTo({ top: 0, behavior: "instant" });
  };
  const logout = async () => {
    try {
      await api("/app/logout", { method: "POST" });
      setAccount(null);
      setHomeId("");
      setData(null);
    } catch (e) {
      setToast({ text: e.message, error: true });
    }
  };
  if (!authChecked)
    return (
      <div className="loading-page">
        <House />
        <span>Opening Home…</span>
      </div>
    );
  if (!account)
    return (
      <div className="sign-in">
        <div className="signin-art">
          <HomeDrawing />
          <span>
            Space to live.
            <br />A home that listens.
          </span>
        </div>
        <div className="signin-content">
          <div className="brand">
            <House />
            <span>
              home<span className="brand-dot">.</span>
            </span>
          </div>
          <h1>Welcome home.</h1>
          <p>
            One place for your rooms,
            <br />
            your comforts, your everyday.
          </p>
          {error && (
            <p className="form-error" role="alert">
              {error}
            </p>
          )}
          {new URLSearchParams(location.search).has("signin") && (
            <p className="form-error" role="alert">
              {new URLSearchParams(location.search).get("signin") === "access"
                ? "Your account has not been invited to this home yet."
                : "Sign-in could not be completed. Please try again."}
            </p>
          )}
          <a href="/app/login" className="primary">
            Sign in <ArrowRight size={18} />
          </a>
          <span className="small muted">Fluxzero Home</span>
        </div>
      </div>
    );
  if (!account.homes.length)
    return (
      <Empty
        title="No homes yet"
        action={
          <button className="secondary" onClick={logout}>
            Sign out
          </button>
        }
      >
        Ask your household owner to grant access.
      </Empty>
    );
  const canControl = data?.permission !== "VIEW";
  const canManage = data?.permission === "MANAGE";
  const activeRooms =
    data?.spaces.filter(
      (s) =>
        s.details.kind === "ROOM" ||
        s.details.kind === "OUTDOOR" ||
        data.devices.some((d) => d.device.spaceId === s.id),
    ) || [];
  const selectedIds = roomIds(data?.spaces || [], room);
  const devices =
    data?.devices.filter(
      (v) =>
        selectedIds.has(v.device.spaceId) &&
        v.device.details.name.toLowerCase().includes(search.toLowerCase()),
    ) || [];
  const nameOfRoom = (id) =>
    data?.spaces.find((s) => s.id === id)?.details.name || "Room";
  const zone = data?.home.timeZone || "UTC";
  const nextRoutine = data?.routines
    .filter((r) => r.enabled && r.nextRun)
    .sort((a, b) => a.nextRun.localeCompare(b.nextRun))[0];
  const viewDevice =
    dialog?.type === "device"
      ? data?.devices.find((d) => d.device.deviceId === dialog.id)
      : null;
  return (
    <div className="app-shell">
      {navOpen && (
        <button
          className="nav-scrim"
          aria-label="Close menu"
          onClick={() => setNavOpen(false)}
        />
      )}
      <aside className={`sidebar ${navOpen ? "open" : ""}`}>
        <button className="brand" onClick={() => navigate("overview")}>
          <House size={25} />
          <span>
            home<span className="brand-dot">.</span>
          </span>
        </button>
        <label className="home-switch">
          <span className="eyebrow">YOUR SPACE</span>
          <select
            aria-label="Choose home"
            value={homeId}
            onChange={(e) => {
              setHomeId(e.target.value);
              navigate("overview");
            }}
          >
            {account.homes.map(({ home }) => (
              <option key={home.id} value={home.id}>
                {home.details.name}
              </option>
            ))}
          </select>
        </label>
        <nav aria-label="Main navigation">
          {[
            ["overview", LayoutGrid, "Overview"],
            ["rooms", DoorClosed, "Rooms"],
            ["scenes", Sparkles, "Scenes"],
            ["routines", CalendarClock, "Routines"],
          ].map(([id, Icon, label]) => (
            <button
              key={id}
              className={page === id ? "nav-item active" : "nav-item"}
              aria-current={page === id ? "page" : undefined}
              onClick={() => navigate(id)}
            >
              <Icon size={19} />
              {label}
              {page === id && <span className="nav-dot" />}
            </button>
          ))}
        </nav>
        <nav className="sidebar-devices" aria-label="Devices">
          <span className="eyebrow">DEVICES</span>
          <button
            className={`room-link ${page === "devices" && !room ? "selected" : ""}`}
            aria-current={page === "devices" && !room ? "page" : undefined}
            onClick={() => navigate("devices")}
          >
            <span className="room-bullet" />
            All devices
            <ChevronRight size={13} />
          </button>
          {activeRooms.map((s) => (
            <button
              key={s.id}
              className={`room-link ${room === s.id && page === "devices" ? "selected" : ""}`}
              aria-current={
                page === "devices" && room === s.id ? "page" : undefined
              }
              onClick={() => navigate("devices", s.id)}
            >
              <span className="room-bullet" />
              {s.details.name}
              <ChevronRight size={13} />
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <button
            className="nav-item"
            onClick={() => setDialog({ type: "connections" })}
          >
            <Wifi size={18} />
            Connections
          </button>
          <ProfileMenu
            name={account.name}
            role={
              data?.permission === "VIEW"
                ? "View-only access"
                : data?.permission === "CONTROL"
                  ? "Household member"
                  : "Household manager"
            }
            onSignOut={logout}
          />
        </div>
      </aside>
      <main className="workspace">
        <header className="topbar">
          <IconButton
            label="Open menu"
            className="icon-button mobile-menu"
            onClick={() => setNavOpen(true)}
          >
            <Menu size={21} />
          </IconButton>
          <div className="breadcrumb">
            <House size={15} />
            <span>{data?.home.details.name || "Your home"}</span>
            <ChevronRight size={13} />
            <strong>
              {titleCase(page)}
              {page === "devices" && room ? ` / ${nameOfRoom(room)}` : ""}
            </strong>
          </div>
        </header>
        {error && (
          <div className="banner" role="alert">
            <CircleAlert size={18} />
            <span>{error}</span>
            <button onClick={() => reload().catch((e) => setError(e.message))}>
              Retry
            </button>
          </div>
        )}
        {!data ? (
          <div className="loading-content">
            <div className="skeleton wide-skeleton" />
            <div className="skeleton-grid">
              {[1, 2, 3, 4].map((n) => (
                <div key={n} className="skeleton" />
              ))}
            </div>
          </div>
        ) : (
          <div className="page-content">
            <div className="page-heading">
              <div>
                <span className="eyebrow">
                  {new Intl.DateTimeFormat("en-GB", {
                    timeZone: zone,
                    weekday: "long",
                    day: "numeric",
                    month: "long",
                  }).format(new Date())}
                </span>
                <h1>
                  {page === "devices" && room
                    ? nameOfRoom(room)
                    : titleCase(page)}
                </h1>
              </div>
              <div className="heading-actions">
                {page === "scenes" && canManage && (
                  <button
                    className="primary"
                    onClick={() => setDialog({ type: "scene" })}
                  >
                    <Plus size={17} />
                    New scene
                  </button>
                )}
                {page === "routines" && canManage && (
                  <button
                    className="primary"
                    disabled={!data.scenes.length}
                    onClick={() => setDialog({ type: "routine" })}
                  >
                    <Plus size={17} />
                    New routine
                  </button>
                )}
                {page === "overview" && (
                  <div className="mode-picker" aria-label="Home mode">
                    {Object.entries(modes).map(([mode, [Icon, label]]) => (
                      <button
                        key={mode}
                        title={label}
                        aria-label={`${label} mode`}
                        aria-pressed={data.home.mode === mode}
                        disabled={!canControl || busy.has("mode")}
                        className={data.home.mode === mode ? "selected" : ""}
                        onClick={() =>
                          act("mode", "/mode", { mode }, "PUT").catch(() => {})
                        }
                      >
                        <Icon size={16} />
                        <span>{label}</span>
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </div>
            {page === "overview" && (
              <>
                <HomeSummary
                  data={data}
                  nextRoutine={nextRoutine}
                  navigate={navigate}
                  openConnections={() => setDialog({ type: "connections" })}
                />
                {data.devices.some((v) => !v.delivery) && (
                  <div className="connection-notice">
                    <Wifi size={17} />
                    <span>
                      {data.devices.every((v) => !v.delivery)
                        ? "No devices linked. Requests are saved only."
                        : "Some devices are not linked. Their requests are saved only."}
                    </span>
                    <button onClick={() => setDialog({ type: "connections" })}>
                      Connection details <ChevronRight size={15} />
                    </button>
                  </div>
                )}
                <SectionTitle
                  title="Scenes"
                  action="All scenes"
                  onClick={() => navigate("scenes")}
                />
                <div className="scene-grid">
                  {data.scenes.slice(0, 3).map((scene) => (
                    <SceneCard
                      key={scene.sceneId}
                      scene={scene}
                      busy={busy.has(scene.sceneId)}
                      disabled={!canControl}
                      onActivate={() => activate(scene)}
                    />
                  ))}
                  {!data.scenes.length && (
                    <Empty title="No scenes yet" icon={Sparkles} />
                  )}
                </div>
              </>
            )}
            {page === "rooms" && (
              <RoomsOverview
                rooms={activeRooms}
                data={data}
                navigate={navigate}
              />
            )}
            {page === "devices" && room && (
              <button
                className="text-link room-back"
                onClick={() => navigate("devices")}
              >
                <ArrowLeft size={16} /> All devices
              </button>
            )}
            {(page === "overview" || page === "devices") && (
              <>
                <div className="devices-heading">
                  {page === "devices" && !room ? (
                    <span className="device-total">
                      {devices.length}{" "}
                      {devices.length === 1 ? "device" : "devices"}
                    </span>
                  ) : (
                    <h2>
                      Devices <span className="count">{devices.length}</span>
                    </h2>
                  )}
                  <label className="search">
                    <Search size={17} />
                    <input
                      placeholder="Find a device"
                      aria-label="Find a device"
                      value={search}
                      onChange={(e) => setSearch(e.target.value)}
                    />
                    {search && (
                      <button
                        aria-label="Clear search"
                        onClick={() => setSearch("")}
                      >
                        <X size={14} />
                      </button>
                    )}
                  </label>
                </div>
                <div className="device-grid">
                  {devices.map((v) => (
                    <DeviceCard
                      key={v.device.deviceId}
                      view={v}
                      roomName={nameOfRoom(v.device.spaceId)}
                      disabled={!canControl}
                      busy={busy.has(v.device.deviceId)}
                      onOpen={() =>
                        setDialog({ type: "device", id: v.device.deviceId })
                      }
                      control={(route, body) =>
                        control(v.device.deviceId, route, body)
                      }
                    />
                  ))}
                </div>
                {!devices.length && (
                  <Empty
                    icon={Search}
                    title={search ? "No devices found" : "No devices"}
                  >
                    {search
                      ? "Try another name or room."
                      : "This space has no devices yet."}
                  </Empty>
                )}
              </>
            )}
            {page === "scenes" && (
              <>
                <div className="scene-grid full-scenes">
                  {data.scenes.map((scene) => (
                    <SceneCard
                      key={scene.sceneId}
                      scene={scene}
                      busy={busy.has(scene.sceneId)}
                      disabled={!canControl}
                      onActivate={() => activate(scene)}
                      onEdit={
                        canManage
                          ? () => setDialog({ type: "scene", scene })
                          : null
                      }
                    />
                  ))}
                </div>
                {!data.scenes.length && (
                  <Empty title="Your first scene" icon={Sparkles}>
                    Bring several devices together in one tap.
                  </Empty>
                )}
              </>
            )}
            {page === "routines" && (
              <>
                <div className="routine-meta">
                  <span>
                    <Clock3 size={15} />
                    {zone.replaceAll("_", " ")}
                  </span>
                  <span>
                    {data.routines.filter((r) => r.enabled && r.nextRun).length}{" "}
                    scheduled
                  </span>
                </div>
                <div className="routine-list">
                  {data.routines.map((r) => (
                    <div
                      className={`routine-row ${!r.enabled ? "paused" : ""}`}
                      key={r.routineId}
                    >
                      <div className="routine-time">
                        {r.timing.kind === "weekly"
                          ? r.timing.time.slice(0, 5)
                          : timeIn(r.timing.at, zone)}
                        <span>
                          {r.timing.kind === "weekly" ? "REPEATS" : "ONE TIME"}
                        </span>
                      </div>
                      <div className="routine-info">
                        <h3>{r.details.name}</h3>
                        <p>
                          {data.scenes.find((s) => s.sceneId === r.sceneId)
                            ?.details.name || "Scene unavailable"}{" "}
                          <span>·</span> {timingLabel(r.timing, zone, false)}
                        </p>
                        {r.problem ? (
                          <span className="problem">
                            <CircleAlert size={14} />
                            {r.problem}
                          </span>
                        ) : (
                          <small>
                            {r.enabled && r.nextRun
                              ? `Next ${dayIn(r.nextRun, zone)}`
                              : r.enabled
                                ? "Completed"
                                : "Paused"}
                          </small>
                        )}
                      </div>
                      {!(r.enabled && !r.nextRun) && (
                        <Toggle
                          label={`${r.enabled ? "Pause" : "Resume"} ${r.details.name}`}
                          checked={r.enabled && !!r.nextRun}
                          disabled={!canManage || busy.has(r.routineId)}
                          onClick={() =>
                            act(
                              r.routineId,
                              `/routines/${encodeURIComponent(r.routineId)}/${r.enabled ? "pause" : "resume"}`,
                            ).catch(() => {})
                          }
                        />
                      )}
                      {canManage && (
                        <IconButton
                          label={`Edit ${r.details.name}`}
                          onClick={() =>
                            setDialog({ type: "routine", routine: r })
                          }
                        >
                          <Pencil size={17} />
                        </IconButton>
                      )}
                    </div>
                  ))}
                </div>
                {!data.routines.length && (
                  <Empty
                    icon={CalendarClock}
                    title="No routines yet"
                    action={
                      canManage && data.scenes.length > 0 ? (
                        <button
                          className="secondary"
                          onClick={() => setDialog({ type: "routine" })}
                        >
                          Add a routine
                        </button>
                      ) : null
                    }
                  >
                    Schedule a scene once, or make it part of your week.
                  </Empty>
                )}
              </>
            )}
          </div>
        )}
      </main>
      {toast && (
        <div
          className={`toast ${toast.error ? "error" : ""}`}
          role={toast.error ? "alert" : "status"}
        >
          {toast.error ? <CircleAlert size={18} /> : <Check size={18} />}
          <span>{toast.text}</span>
          <IconButton
            label="Dismiss notification"
            onClick={() => setToast(null)}
          >
            <X size={16} />
          </IconButton>
        </div>
      )}
      {viewDevice && (
        <Dialog
          title={viewDevice.device.details.name}
          onClose={() => setDialog(null)}
        >
          <DeviceDetail
            view={viewDevice}
            roomName={nameOfRoom(viewDevice.device.spaceId)}
            zone={zone}
            disabled={!canControl || busy.has(viewDevice.device.deviceId)}
            control={(route, body) =>
              control(viewDevice.device.deviceId, route, body)
            }
          />
        </Dialog>
      )}
      {dialog?.type === "routine" && data && (
        <RoutineEditor
          routine={dialog.routine}
          data={data}
          close={() => setDialog(null)}
          save={(id, body) =>
            act(
              id,
              `/routines/${encodeURIComponent(id)}`,
              body,
              "PUT",
              "Routine saved",
            )
          }
          remove={(id) =>
            act(
              id,
              `/routines/${encodeURIComponent(id)}`,
              undefined,
              "DELETE",
              "Routine removed",
            )
          }
        />
      )}
      {dialog?.type === "scene" && data && (
        <SceneEditor
          scene={dialog.scene}
          data={data}
          close={() => setDialog(null)}
          save={(id, body) =>
            act(
              id,
              `/scenes/${encodeURIComponent(id)}`,
              body,
              "PUT",
              "Scene saved",
            )
          }
          remove={(id) =>
            act(
              id,
              `/scenes/${encodeURIComponent(id)}`,
              undefined,
              "DELETE",
              "Scene removed",
            )
          }
        />
      )}
      {dialog?.type === "connections" && data && (
        <Dialog title="Connections" onClose={() => setDialog(null)}>
          <div className="connection-summary">
            <span className="round-icon sage">
              <Wifi size={24} />
            </span>
            <div>
              <h3>Home Assistant</h3>
              <p>
                {data.devices.filter((v) => v.delivery).length} linked devices
              </p>
            </div>
          </div>
          {data.devices
            .filter((v) => v.delivery)
            .map((v) => (
              <div className="connection-row" key={v.device.deviceId}>
                <span>{v.device.details.name}</span>
                <small className={v.delivery.problem ? "problem" : "muted"}>
                  {v.delivery.problem || "Linked"}
                </small>
              </div>
            ))}
          {!data.devices.some((v) => v.delivery) && (
            <div className="note">
              No physical devices are linked. You can save settings and scenes,
              but they will not control equipment yet.
            </div>
          )}
          <p className="connection-help">
            Device setup is not available in this interface yet. Your household
            administrator can connect Home Assistant and link its devices.
          </p>
          {canManage && (
            <details className="setup-help">
              <summary>Administrator setup</summary>
              <p>
                Follow <code>docs/home-assistant.md</code> in the example
                repository to configure Home Assistant, discover its entities
                and link them to this home. Keep access tokens in the server
                configuration.
              </p>
              <a
                className="text-link"
                href="/api/docs"
                target="_blank"
                rel="noreferrer"
              >
                Developer API reference <ArrowUpRight size={14} />
              </a>
            </details>
          )}
        </Dialog>
      )}
    </div>
  );
}
