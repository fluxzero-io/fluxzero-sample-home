import { useCallback, useEffect, useRef, useState } from "react";
import {
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
  Lightbulb,
  LogOut,
  Menu,
  Moon,
  Pencil,
  Plus,
  Search,
  SlidersHorizontal,
  Sparkles,
  Sun,
  Thermometer,
  Wifi,
  X,
} from "lucide-react";
import {
  dateIn,
  requestedOn,
  roomIds,
  timeIn,
  timingLabel,
  titleCase,
} from "./home.js";
import {
  IconButton,
  Empty,
  Dialog,
  Toggle,
  SectionTitle,
  HomeDrawing,
} from "./ui.jsx";
import { api } from "./api.js";
import { DeviceCard, DeviceDetail } from "./Devices.jsx";
import { SceneCard, SceneEditor } from "./Scenes.jsx";
import { RoutineEditor } from "./Routines.jsx";

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
  const [live, setLive] = useState("connecting");
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
      setLive("connecting");
      socket = new WebSocket(
        `${location.protocol === "https:" ? "wss" : "ws"}://${location.host}/api/homes/${encodeURIComponent(homeId)}/live`,
      );
      socket.onopen = () => {
        attempt = 0;
        setLive("live");
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
          setLive("reconnecting");
        }
      };
      socket.onclose = (event) => {
        clearInterval(heartbeat);
        if (stopped) return;
        setLive("reconnecting");
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
              onClick={() => navigate(id)}
            >
              <Icon size={19} />
              {label}
              {page === id && <span className="nav-dot" />}
            </button>
          ))}
        </nav>
        <div className="sidebar-rooms">
          <span className="eyebrow">ROOMS</span>
          {activeRooms.map((s) => (
            <button
              key={s.id}
              className={`room-link ${room === s.id && page === "rooms" ? "selected" : ""}`}
              onClick={() => navigate("rooms", s.id)}
            >
              <span className="room-bullet" />
              {s.details.name}
              <ChevronRight size={13} />
            </button>
          ))}
        </div>
        <div className="sidebar-bottom">
          <button
            className="nav-item"
            onClick={() => setDialog({ type: "connections" })}
          >
            <Wifi size={18} />
            Connections
          </button>
          <div className="profile">
            <span className="avatar">
              {account.name
                .split(" ")
                .map((s) => s[0])
                .slice(0, 2)
                .join("")}
            </span>
            <div>
              <strong>{account.name}</strong>
              <small>
                {data?.permission === "VIEW"
                  ? "View-only access"
                  : data?.permission === "CONTROL"
                    ? "Household member"
                    : "Household manager"}
              </small>
            </div>
            <IconButton label="Sign out" onClick={logout}>
              <LogOut size={16} />
            </IconButton>
          </div>
          <span className="powered">BUILT WITH FLUXZERO</span>
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
            <strong>{titleCase(page)}</strong>
          </div>
          <div className="topbar-right">
            <span className={`live ${live === "live" ? "" : "connecting"}`}>
              <span />
              {live === "live" ? "Live" : "Reconnecting"}
            </span>
            <button
              className="avatar small-avatar"
              aria-label="Account and connections"
              onClick={() => setDialog({ type: "connections" })}
            >
              {account.name[0]}
            </button>
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
                  {page === "overview"
                    ? "A place to unwind."
                    : page === "rooms"
                      ? room
                        ? nameOfRoom(room)
                        : "Every room. Your way."
                      : page === "scenes"
                        ? "Set the mood."
                        : "Your everyday, on time."}
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
                <div className="overview-grid">
                  <div className="home-hero">
                    <div>
                      <span className="eyebrow">MAKE YOURSELF AT HOME</span>
                      <h2>{data.home.details.name}</h2>
                      <p>
                        {activeRooms.length} rooms <span>·</span>{" "}
                        {data.devices.length} devices
                      </p>
                      <button
                        className="hero-link"
                        onClick={() => navigate("rooms")}
                      >
                        Explore your spaces <ArrowUpRight size={16} />
                      </button>
                    </div>
                    <HomeDrawing />
                  </div>
                  <div className="at-a-glance">
                    <div className="section-caption">
                      <span className="eyebrow">AT A GLANCE</span>
                      <SlidersHorizontal size={15} />
                    </div>
                    <div className="glance-row">
                      <span className="round-icon warm">
                        <Lightbulb size={20} />
                      </span>
                      <div>
                        <span>Lighting</span>
                        <strong>
                          {
                            data.devices
                              .filter(
                                (v) =>
                                  v.device.capabilities.includes(
                                    "LIGHT_LEVEL",
                                  ) || v.device.capabilities.includes("POWER"),
                              )
                              .filter((v) => requestedOn(v.device)).length
                          }{" "}
                          <small>requested on</small>
                        </strong>
                      </div>
                    </div>
                    <div className="glance-row">
                      <span className="round-icon sage">
                        <Thermometer size={20} />
                      </span>
                      <div>
                        <span>Climate</span>
                        <strong>
                          {(() => {
                            const v = data.devices.find(
                              (v) =>
                                v.status?.availability === "ONLINE" &&
                                v.status?.readings?.TEMPERATURE != null,
                            );
                            return v ? (
                              <>
                                {v.status.readings.TEMPERATURE}°{" "}
                                <small>{nameOfRoom(v.device.spaceId)}</small>
                              </>
                            ) : (
                              <>
                                — <small>No reading yet</small>
                              </>
                            );
                          })()}
                        </strong>
                      </div>
                    </div>
                    <button
                      className="next-up"
                      onClick={() => navigate("routines")}
                    >
                      <Clock3 size={17} />
                      <div>
                        <span>Next up</span>
                        <strong>
                          {nextRoutine
                            ? `${timeIn(nextRoutine.nextRun, zone)} · ${nextRoutine.details.name}`
                            : "Nothing scheduled"}
                        </strong>
                      </div>
                      <ChevronRight size={16} />
                    </button>
                  </div>
                </div>
                <SectionTitle
                  title="A moment for every mood"
                  action="All scenes"
                  onClick={() => navigate("scenes")}
                />
                <div className="scene-grid">
                  {data.scenes.slice(0, 3).map((scene, i) => (
                    <SceneCard
                      key={scene.sceneId}
                      scene={scene}
                      index={i}
                      busy={busy.has(scene.sceneId)}
                      disabled={!canControl}
                      onActivate={() => activate(scene)}
                    />
                  ))}
                  {!data.scenes.length && (
                    <Empty title="Make room for a scene" icon={Sparkles} />
                  )}
                </div>
              </>
            )}
            {(page === "overview" || page === "rooms") && (
              <>
                <div className="devices-heading">
                  <h2>
                    {page === "overview"
                      ? "Your devices"
                      : room
                        ? "In this space"
                        : "All devices"}{" "}
                    <span className="count">{devices.length}</span>
                  </h2>
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
                <div className="room-tabs" aria-label="Filter by room">
                  <button
                    className={!room ? "active" : ""}
                    onClick={() => setRoom("")}
                  >
                    All rooms
                  </button>
                  {activeRooms.map((s) => (
                    <button
                      key={s.id}
                      className={room === s.id ? "active" : ""}
                      onClick={() => setRoom(s.id)}
                    >
                      {s.details.name}
                    </button>
                  ))}
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
                    title={search ? "No devices found" : "A little quiet here"}
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
                  {data.scenes.map((scene, i) => (
                    <SceneCard
                      key={scene.sceneId}
                      scene={scene}
                      index={i}
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
                          <span>·</span> {timingLabel(r.timing, zone)}
                        </p>
                        {r.problem ? (
                          <span className="problem">
                            <CircleAlert size={14} />
                            {r.problem}
                          </span>
                        ) : (
                          <small>
                            {r.enabled && r.nextRun
                              ? `Next ${dateIn(r.nextRun, zone)}`
                              : r.enabled
                                ? "Completed"
                                : "Paused"}
                          </small>
                        )}
                      </div>
                      <Toggle
                        label={`${r.enabled ? "Pause" : "Resume"} ${r.details.name}`}
                        checked={r.enabled && !!r.nextRun}
                        disabled={
                          !canManage ||
                          busy.has(r.routineId) ||
                          (r.enabled && !r.nextRun)
                        }
                        onClick={() =>
                          act(
                            r.routineId,
                            `/routines/${encodeURIComponent(r.routineId)}/${r.enabled ? "pause" : "resume"}`,
                          ).catch(() => {})
                        }
                      />
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
                    title="Let your home remember"
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
            <footer className="page-footer">
              <span>
                <span className="tiny-dot" />
                Your home, connected.
              </span>
              <span>
                Fluxzero Home <span className="footer-divider">/</span>{" "}
                {data.permission === "VIEW"
                  ? "View only"
                  : "Designed around you"}
              </span>
            </footer>
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
              This example home is ready to connect. Device requests are saved;
              physical control starts after an operator links Home Assistant.
            </div>
          )}
          <a
            className="text-link"
            href="/api/docs"
            target="_blank"
            rel="noreferrer"
          >
            API reference <ArrowUpRight size={14} />
          </a>
        </Dialog>
      )}
    </div>
  );
}
