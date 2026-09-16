import { useEffect, useId, useRef, useState } from "react";
import { ArrowDown, ArrowRight, House, X } from "lucide-react";

export function IconButton({ label, children, ...props }) {
  return (
    <button
      className="icon-button"
      type="button"
      aria-label={label}
      title={label}
      {...props}
    >
      {children}
    </button>
  );
}

export function Empty({ icon: Icon = House, title, children, action }) {
  return (
    <div className="empty">
      <Icon size={30} />
      <h3>{title}</h3>
      {children && <p>{children}</p>}
      {action}
    </div>
  );
}

export function Dialog({ title, children, onClose, wide = false }) {
  const ref = useRef(null);
  const id = useId();
  useEffect(() => {
    const el = ref.current;
    el.showModal();
    return () => {
      if (el.open) el.close();
    };
  }, []);
  return (
    <dialog
      ref={ref}
      className={wide ? "dialog wide" : "dialog"}
      aria-labelledby={id}
      onCancel={(e) => {
        e.preventDefault();
        onClose();
      }}
      onClick={(e) => {
        if (e.target === ref.current) {
          const r = ref.current.getBoundingClientRect();
          if (
            e.clientX < r.left ||
            e.clientX > r.right ||
            e.clientY < r.top ||
            e.clientY > r.bottom
          )
            onClose();
        }
      }}
    >
      <div className="dialog-head">
        <h2 id={id}>{title}</h2>
        <IconButton label="Close" onClick={onClose}>
          <X size={20} />
        </IconButton>
      </div>
      {children}
    </dialog>
  );
}

export function Range({
  label,
  value,
  min = 0,
  max = 100,
  step = 1,
  unit = "%",
  disabled,
  commit,
}) {
  const [draft, setDraft] = useState(value ?? min);
  const active = useRef(false);
  const dirty = useRef(false);
  const last = useRef(value);
  const id = useId();
  useEffect(() => {
    if (!active.current) {
      setDraft(value ?? min);
      last.current = value;
    }
  }, [value, min]);
  const submit = async () => {
    active.current = false;
    if (!dirty.current) return;
    dirty.current = false;
    if (draft !== last.current) {
      const previous = last.current;
      last.current = draft;
      try {
        await commit(draft);
      } catch {
        last.current = previous;
        setDraft(previous ?? min);
      }
    }
  };
  return (
    <div className="range-field">
      <div>
        <label htmlFor={id}>{label}</label>
        <output htmlFor={id}>
          {last.current == null && !dirty.current ? "—" : `${draft}${unit}`}
        </output>
      </div>
      <input
        id={id}
        type="range"
        min={min}
        max={max}
        step={step}
        value={draft}
        disabled={disabled}
        style={{ "--range": `${((draft - min) / (max - min)) * 100}%` }}
        onPointerDown={() => {
          active.current = true;
        }}
        onChange={(e) => {
          active.current = true;
          dirty.current = true;
          setDraft(Number(e.target.value));
        }}
        onPointerUp={submit}
        onKeyUp={(e) => {
          if (
            [
              "ArrowLeft",
              "ArrowRight",
              "ArrowUp",
              "ArrowDown",
              "Home",
              "End",
              "PageUp",
              "PageDown",
            ].includes(e.key)
          )
            submit();
        }}
        onBlur={submit}
      />
    </div>
  );
}

export function Toggle({ label, checked, disabled, onClick }) {
  return (
    <button
      type="button"
      className={`toggle ${checked ? "on" : ""}`}
      role="switch"
      aria-checked={checked}
      aria-label={label}
      disabled={disabled}
      onClick={onClick}
    >
      <span />
    </button>
  );
}

export function SectionTitle({ title, action, onClick }) {
  return (
    <div className="section-title">
      <h2>{title}</h2>
      {action && (
        <button onClick={onClick}>
          {action}
          <ArrowRight size={16} />
        </button>
      )}
    </div>
  );
}

export function HomeDrawing() {
  return (
    <svg
      className="home-drawing"
      viewBox="0 0 360 240"
      fill="none"
      aria-hidden="true"
    >
      <path d="M32 201 179 235l157-60-148-34Z" fill="#d7ddcd" />
      <path d="m85 101 104 31v78L85 179Z" fill="#f8f5e9" />
      <path d="m189 132 94-35v77l-94 36Z" fill="#e4e6d7" />
      <path d="m64 105 108-85 132 77-113 43Z" fill="#344c3c" />
      <path d="m64 105 108-85 20 120Z" fill="#49634d" />
      <path d="m119 58 25-19 18 11v41l-25-8Z" fill="#263d30" />
      <path d="m220 153 28-10v45l-28 10Z" fill="#778c72" />
      <path d="m107 127 25 8v29l-25-8Z" fill="#efd999" />
      <path d="m151 140 21 6v28l-21-7Z" fill="#e9d6a3" />
      <path d="m260 127 13-5v24l-13 5Z" fill="#a8b5a0" />
      <path
        d="m120 131 0 29M109 142l21 6M160 143v29"
        stroke="#e3dabb"
        strokeWidth="2"
      />
      <path d="m42 170 0 42m273-54v47" stroke="#536b50" strokeWidth="3" />
      <ellipse cx="42" cy="161" rx="17" ry="27" fill="#718461" />
      <ellipse cx="277" cy="145" rx="15" ry="29" fill="#90a078" />
      <ellipse cx="314" cy="178" rx="13" ry="18" fill="#a4b08c" />
      <path d="m234 193 13 8-44 18-18-8Z" fill="#bbc6ac" />
      <path
        d="m19 214 297-113M49 228l285-110"
        stroke="#c5cfb8"
        strokeWidth=".6"
        opacity=".4"
      />
    </svg>
  );
}
