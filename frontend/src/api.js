export async function api(path, { method = "GET", body, signal } = {}) {
  const response = await fetch(path, {
    method,
    credentials: "same-origin",
    signal,
    headers: {
      Accept: "application/json",
      ...(method !== "GET" ? { "X-Home-Request": "1" } : {}),
      ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  if (!response.ok) {
    const text = await response.text();
    let message;
    try {
      const error = JSON.parse(text);
      message = error.message || error.error;
    } catch {
      /* Plain-text functional errors are valid API responses. */
    }
    const error = new Error(
      message || text || "That change could not be saved. Please try again.",
    );
    error.status = response.status;
    throw error;
  }
  return response.status === 204 ? null : response.json();
}
