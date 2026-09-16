import { useState } from "react";
import { Check, Trash2 } from "lucide-react";
import { DAYS, titleCase } from "./home.js";
import { IconButton, Dialog } from "./ui.jsx";

export function RoutineEditor({ routine, data, close, save, remove }) {
  const [name, setName] = useState(routine?.details.name || "");
  const [scene, setScene] = useState(
    routine?.sceneId || data.scenes[0]?.sceneId || "",
  );
  const [kind, setKind] = useState(routine?.timing.kind || "weekly");
  const [days, setDays] = useState(routine?.timing.days || DAYS.slice(0, 5));
  const [time, setTime] = useState(
    routine?.timing.time?.slice(0, 5) || "20:00",
  );
  const localDate = (at) => {
    const d = new Date(at);
    d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
    return d.toISOString().slice(0, 16);
  };
  const [at, setAt] = useState(
    routine?.timing.at
      ? localDate(routine.timing.at)
      : localDate(Date.now() + 3600000),
  );
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  async function submit(e) {
    e.preventDefault();
    setPending(true);
    setError("");
    try {
      await save(routine?.routineId || crypto.randomUUID(), {
        details: { name },
        sceneId: scene,
        timing:
          kind === "weekly"
            ? { kind, days, time: `${time}:00` }
            : { kind, at: new Date(at).toISOString() },
      });
      close();
    } catch (e) {
      setError(e.message);
    } finally {
      setPending(false);
    }
  }
  async function erase() {
    if (!confirm(`Remove “${name}”?`)) return;
    setPending(true);
    try {
      await remove(routine.routineId);
      close();
    } catch (e) {
      setError(e.message);
      setPending(false);
    }
  }
  return (
    <Dialog title={routine ? "Edit routine" : "A new rhythm"} onClose={close}>
      <form onSubmit={submit}>
        <div className="field">
          <label htmlFor="routine-name">Name</label>
          <input
            id="routine-name"
            required
            maxLength={120}
            placeholder="Evening comfort"
            value={name}
            onChange={(e) => setName(e.target.value)}
            autoFocus
          />
        </div>
        <div className="field">
          <label htmlFor="routine-scene">Scene</label>
          <select
            id="routine-scene"
            required
            value={scene}
            onChange={(e) => setScene(e.target.value)}
          >
            {data.scenes.map((s) => (
              <option key={s.sceneId} value={s.sceneId}>
                {s.details.name}
              </option>
            ))}
          </select>
        </div>
        <div className="segmented">
          <button
            type="button"
            className={kind === "weekly" ? "selected" : ""}
            onClick={() => setKind("weekly")}
          >
            Weekly
          </button>
          <button
            type="button"
            className={kind === "once" ? "selected" : ""}
            onClick={() => setKind("once")}
          >
            Once
          </button>
        </div>
        {kind === "weekly" ? (
          <>
            <div className="field">
              <label>Repeat on</label>
              <div className="day-picker">
                {DAYS.map((d) => (
                  <button
                    type="button"
                    key={d}
                    title={titleCase(d)}
                    aria-label={titleCase(d)}
                    aria-pressed={days.includes(d)}
                    className={days.includes(d) ? "selected" : ""}
                    onClick={() =>
                      setDays((prev) =>
                        prev.includes(d)
                          ? prev.filter((x) => x !== d)
                          : [...prev, d],
                      )
                    }
                  >
                    {d.slice(0, 2)}
                  </button>
                ))}
              </div>
            </div>
            <div className="field">
              <label htmlFor="routine-time">At</label>
              <input
                id="routine-time"
                type="time"
                required
                value={time}
                onChange={(e) => setTime(e.target.value)}
              />
              <small>{data.home.timeZone.replaceAll("_", " ")}</small>
            </div>
          </>
        ) : (
          <div className="field">
            <label htmlFor="routine-date">Date & time</label>
            <input
              id="routine-date"
              type="datetime-local"
              required
              value={at}
              onChange={(e) => setAt(e.target.value)}
            />
            <small>
              Your timezone:{" "}
              {Intl.DateTimeFormat()
                .resolvedOptions()
                .timeZone.replaceAll("_", " ")}
            </small>
          </div>
        )}
        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}
        <div className="form-actions">
          {routine && (
            <IconButton
              label="Remove routine"
              disabled={pending}
              onClick={erase}
            >
              <Trash2 size={18} />
            </IconButton>
          )}
          <button
            type="button"
            className="secondary"
            disabled={pending}
            onClick={close}
          >
            Cancel
          </button>
          <button
            className="primary"
            disabled={
              pending ||
              !name.trim() ||
              !scene ||
              (kind === "weekly" && !days.length)
            }
          >
            {pending ? "Saving…" : routine ? "Save routine" : "Create routine"}
            <Check size={16} />
          </button>
        </div>
      </form>
    </Dialog>
  );
}
