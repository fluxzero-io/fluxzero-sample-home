import { useState } from "react";
import { Dialog } from "./ui.jsx";

export function RoomEditor({ data, close, save, created }) {
  const [name, setName] = useState("");
  const [parent, setParent] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  async function submit(event) {
    event.preventDefault();
    setPending(true);
    setError("");
    try {
      const result = await save({
        details: { name: name.trim(), kind: "ROOM" },
        enclosingSpaceId: parent || null,
      });
      close();
      created(result.spaceId);
    } catch (failure) {
      setError(failure.message);
    } finally {
      setPending(false);
    }
  }
  return (
    <Dialog title="New room" onClose={close}>
      <form onSubmit={submit}>
        <div className="field">
          <label htmlFor="room-name">Name</label>
          <input
            id="room-name"
            autoFocus
            required
            maxLength={120}
            placeholder="Study"
            value={name}
            onChange={(event) => setName(event.target.value)}
            disabled={pending}
          />
        </div>
        <div className="field">
          <label htmlFor="room-parent">Location</label>
          <select
            id="room-parent"
            value={parent}
            onChange={(event) => setParent(event.target.value)}
            disabled={pending}
          >
            <option value="">{data.home.details.name}</option>
            {data.spaces.map((space) => (
              <option key={space.id} value={space.id}>
                {space.details.name}
              </option>
            ))}
          </select>
        </div>
        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}
        <div className="form-actions">
          <button
            className="secondary"
            type="button"
            disabled={pending}
            onClick={close}
          >
            Cancel
          </button>
          <button className="primary" disabled={pending || !name.trim()}>
            {pending ? "Creating…" : "Create room"}
          </button>
        </div>
      </form>
    </Dialog>
  );
}
