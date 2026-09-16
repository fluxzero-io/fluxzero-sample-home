import { useState } from "react";
import {
  ArrowDown,
  ArrowUp,
  Check,
  Pencil,
  Play,
  Plus,
  Sparkles,
  Trash2,
  X,
} from "lucide-react";
import { CAPABILITIES, sceneSummary, setting, titleCase } from "./home.js";
import { IconButton, Empty, Dialog } from "./ui.jsx";

export function SceneCard({ scene, busy, disabled, onActivate, onEdit }) {
  const Icon = Sparkles;
  return (
    <div className="scene-card">
      <button
        className="scene-activate"
        onClick={onActivate}
        disabled={disabled || busy}
      >
        <span className="scene-icon">
          <Icon size={25} strokeWidth={1.5} />
        </span>
        <span>
          <strong>{scene.details.name}</strong>
          <small>{busy ? "Requesting…" : sceneSummary(scene)}</small>
        </span>
        <span className="scene-play">
          <Play size={15} />
        </span>
      </button>
      {onEdit && (
        <IconButton label={`Edit ${scene.details.name}`} onClick={onEdit}>
          <Pencil size={18} />
        </IconButton>
      )}
    </div>
  );
}

export function SceneEditor({ scene, data, close, save, remove }) {
  const [name, setName] = useState(scene?.details.name || "");
  const [actions, setActions] = useState(scene?.actions || []);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const add = () => {
    const device = data.devices.find(
      (v) => v.device.capabilities.length,
    )?.device;
    if (!device) return;
    const c = CAPABILITIES[device.capabilities[0]];
    setActions((prev) => [
      ...prev,
      {
        kind: c.kind,
        target: { kind: "device", deviceId: device.deviceId },
        [c.field]: { ...c.value },
      },
    ]);
  };
  const change = (i, action) =>
    setActions((prev) => prev.map((a, n) => (i === n ? action : a)));
  async function submit(e) {
    e.preventDefault();
    setPending(true);
    try {
      await save(scene?.sceneId || crypto.randomUUID(), {
        details: { name },
        actions,
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
      await remove(scene.sceneId);
      close();
    } catch (e) {
      setError(e.message);
      setPending(false);
    }
  }
  return (
    <Dialog title={scene ? "Edit scene" : "New scene"} onClose={close} wide>
      <form onSubmit={submit}>
        <div className="field">
          <label htmlFor="scene-name">Name</label>
          <input
            autoFocus
            id="scene-name"
            placeholder="A slow Sunday"
            required
            maxLength={120}
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </div>
        <div className="section-title">
          <h3>Actions</h3>
          <button
            type="button"
            onClick={add}
            disabled={!data.devices.some((v) => v.device.capabilities.length)}
          >
            <Plus size={16} />
            Add action
          </button>
        </div>
        <div className="scene-actions">
          {actions.map((a, i) => (
            <SceneActionEditor
              key={i}
              action={a}
              index={i}
              data={data}
              change={(v) => change(i, v)}
              remove={() =>
                setActions((prev) => prev.filter((_, n) => n !== i))
              }
              move={(dir) =>
                setActions((prev) => {
                  const next = [...prev];
                  [next[i], next[i + dir]] = [next[i + dir], next[i]];
                  return next;
                })
              }
              last={i === actions.length - 1}
            />
          ))}
        </div>
        {!actions.length && (
          <Empty
            icon={Sparkles}
            title="Start with one action"
            action={
              <button type="button" className="secondary" onClick={add}>
                Add action
              </button>
            }
          />
        )}
        <p className="small muted">
          Actions run together. Later actions refine earlier ones.
        </p>
        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}
        <div className="form-actions">
          {scene && (
            <IconButton label="Remove scene" disabled={pending} onClick={erase}>
              <Trash2 size={18} />
            </IconButton>
          )}
          <button type="button" className="secondary" onClick={close}>
            Cancel
          </button>
          <button
            className="primary"
            disabled={pending || !name.trim() || !actions.length}
          >
            {pending ? "Saving…" : scene ? "Save scene" : "Create scene"}
            <Check size={16} />
          </button>
        </div>
      </form>
    </Dialog>
  );
}

export function SceneActionEditor({
  action: a,
  index,
  data,
  change,
  remove,
  move,
  last,
}) {
  const entry = Object.entries(CAPABILITIES).find(([, c]) => c.kind === a.kind);
  const [cap, def] = entry || [];
  if (!def)
    return <p className="form-error">This action cannot be edited here.</p>;
  const target = a.target;
  const targetValue =
    target.kind === "home"
      ? "home"
      : `${target.kind}:${target.deviceId || target.spaceId || target.zoneId}`;
  const available =
    target.kind === "device"
      ? data.devices.find((v) => v.device.deviceId === target.deviceId)?.device
          .capabilities || []
      : Object.keys(CAPABILITIES);
  const value = a[def.field];
  const setValue = (v) => change({ ...a, [def.field]: v });
  return (
    <div className="action-editor">
      <div className="action-top">
        <span className="action-number">{index + 1}</span>
        <select
          aria-label={`Action ${index + 1} target`}
          value={targetValue}
          onChange={(e) => {
            const [kind, ...parts] = e.target.value.split(":");
            const id = parts.join(":");
            const t = kind === "home" ? { kind } : { kind, [`${kind}Id`]: id };
            const caps =
              kind === "device"
                ? data.devices.find((v) => v.device.deviceId === id)?.device
                    .capabilities
                : available;
            const c = caps?.includes(cap)
              ? def
              : CAPABILITIES[caps?.[0]] || def;
            change({
              kind: c.kind,
              target: t,
              [c.field]: c === def ? value : { ...c.value },
            });
          }}
        >
          <option value="home">Whole home</option>
          <optgroup label="Devices">
            {data.devices
              .filter((v) => v.device.capabilities.length)
              .map((v) => (
                <option
                  key={v.device.deviceId}
                  value={`device:${v.device.deviceId}`}
                >
                  {v.device.details.name}
                </option>
              ))}
          </optgroup>
          <optgroup label="Spaces">
            {data.spaces.map((s) => (
              <option key={s.id} value={`space:${s.id}`}>
                {s.details.name}
              </option>
            ))}
          </optgroup>
          {data.zones.length > 0 && (
            <optgroup label="Zones">
              {data.zones.map((z) => (
                <option key={z.zoneId} value={`zone:${z.zoneId}`}>
                  {z.details.name}
                </option>
              ))}
            </optgroup>
          )}
        </select>
        <IconButton
          label={`Move action ${index + 1} up`}
          disabled={!index}
          onClick={() => move(-1)}
        >
          <ArrowUp size={15} />
        </IconButton>
        <IconButton
          label={`Move action ${index + 1} down`}
          disabled={last}
          onClick={() => move(1)}
        >
          <ArrowDown size={15} />
        </IconButton>
        <IconButton label={`Remove action ${index + 1}`} onClick={remove}>
          <X size={17} />
        </IconButton>
      </div>
      <div className="action-fields">
        <select
          aria-label={`Action ${index + 1} setting`}
          value={cap}
          onChange={(e) => {
            const c = CAPABILITIES[e.target.value];
            change({ kind: c.kind, target, [c.field]: { ...c.value } });
          }}
        >
          {available.map((c) => (
            <option key={c} value={c}>
              {CAPABILITIES[c]?.label || c}
            </option>
          ))}
        </select>
        {Object.entries(value)
          .filter(([key]) => key !== "kind")
          .map(([key, val]) =>
            typeof val === "boolean" ? (
              <label key={key} className="checkbox">
                <input
                  type="checkbox"
                  checked={val}
                  onChange={(e) =>
                    setValue({ ...value, [key]: e.target.checked })
                  }
                />
                {titleCase(key)}
              </label>
            ) : (
              <label className="compact-field" key={key}>
                <span>
                  {key === "percent"
                    ? "%"
                    : key === "celsius"
                      ? "°C"
                      : titleCase(key)}
                </span>
                <input
                  aria-label={`Action ${index + 1} ${key}`}
                  type={typeof val === "number" ? "number" : "text"}
                  value={val ?? ""}
                  required={key !== "media" || value.playing}
                  min={key === "celsius" ? 5 : 0}
                  max={key === "hue" ? 359 : key === "celsius" ? 35 : 100}
                  step={key === "celsius" ? 0.5 : 1}
                  onChange={(e) =>
                    setValue({
                      ...value,
                      [key]:
                        typeof val === "number"
                          ? Number(e.target.value)
                          : e.target.value,
                    })
                  }
                />
              </label>
            ),
          )}
      </div>
    </div>
  );
}
