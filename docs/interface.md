# The Home interface

The English, responsive interface puts common actions on device tiles and keeps detailed controls in a dialog. Rooms, scenes and routines have their own views. The original visual design uses quiet colors, clear device states and small amounts of text. Interaction references were [Home Assistant tiles](https://www.home-assistant.io/dashboards/tile/) and [Apple Home accessory controls](https://support.apple.com/en-euro/guide/iphone/iph0a717a8fd/ios); the components and artwork are original.

## Development

Run `fz dev` from the repository root and open the public URL it prints. The environment owns the SDK runtime, backend, local identity provider and Vite dev server. Node 22.12+ is required for the frontend. Its initial setup installs locked dependencies, runs the focused frontend checks and builds production assets once. Subsequent React and CSS edits use Vite hot module replacement; they do not require a Java rebuild. Backend edits go through the managed compilation and focused test loop.

Sign in through the local IDP as **alex** to manage the example home, or **sam** to view it. These are development identities provisioned by `examples/11-*` and `12-*`, not production credentials or a public signup route. Unknown identities are refused. A fresh temporary runtime reloads the examples; ordinary application reloads preserve state.

The dev gateway routes `/api`, `/app` and the managed IDP's `/login` to Fluxzero, and everything else to Vite. Its IDP callback may use the `/_fluxzero` mount. The encrypted, short-lived login cookie therefore covers the origin root.

## What is available

- A compact overview with device connections, set and measured room temperature, the next routine, scene intentions and device controls. Rooms has a dedicated overview with **New room** for managers. A room can belong directly to the home or to an existing space; it does not depend on an integration or discovery. Devices in the sidebar opens all devices or filters them by room, including nested spaces and device search. Room cards open the same device view. Pages end with breathing room instead of a footer.
- All current core capabilities: power, brightness, color, temperature, opening, locks, playback, volume, ventilation, irrigation and charging. Tiles show common controls; device details contain the complete set.
- Requested settings alongside reported settings, availability and measurements. An accepted command is an intention, not a physical acknowledgement. The example has no linked equipment and displays **No report** until observations arrive. Power requests are explicit: brightness never implies power on, and an unknown power state uses a left-aligned switch thumb with a question mark. An online power report is used when no power setting exists. Unknown sliders show **Not set** without a position marker. Device tile highlighting comes only from reported online power; controls show the chosen setting directly. For linked devices, a **Syncing** indicator appears only after a mismatch persists for one second and disappears on confirmation. A pending HTTP command uses the same delay. Delivery failures and offline state remain visible; unlinked devices retain **Not linked** instead of an endless spinner. The temperature summary pairs a thermostat setting with an online measurement from the same room, never a different room. An online device with no requested settings is not called confirmed.
- Scene activation and an ordered scene editor. Actions can target a device, a space, a zone or the whole home. The existing domain commits the scene's intentions together.
- One-off and weekly routines, with editing, pause, resume and removal. Weekly times use the home's timezone. One-off entry explicitly names the browser's timezone; the resulting instant and next execution are displayed in the home's timezone. Completed one-off routines can be edited to choose a new time.
- Home Assistant device-link status is available under Connections; the header has no routine connection badge. Unlinked devices can save intentions but cannot control equipment. Connections explains that in-app device setup is not available yet, with administrator guidance kept under a separate disclosure. An operator configures and links the adapter; the browser never receives its credentials. The bottom-left identity is the single account menu, with Sign out. It supports keyboard navigation, Escape and outside-click dismissal.

Further layout changes, residents, automation definitions and adapter provisioning remain command-driven. This slice exposes room creation, viewing and everyday control, scene composition and scheduling.

## Identity and permissions

The BFF follows OIDC authorization code with PKCE through the official Fluxzero IDP client. Only a validated subject with a pre-provisioned `Account` receives a Home session. `Account` is deliberately separate from `Resident`: presence and household role do not grant API access.

A trusted operator sends `GrantHomeAccess(accountId, details, homeId, permission)` as the system user. Use the exact identity-provider subject as `AccountId` input. `RevokeHomeAccess(accountId, homeId)` removes that membership. These provisioning commands have no public HTTP routes and require `SYSTEM`.

| Permission | Read / live view | Devices, home mode, activate scene | Create rooms, define / remove scenes, manage routines |
| --- | --- | --- | --- |
| `VIEW` | Yes | No | No |
| `CONTROL` | Yes | Yes | No |
| `MANAGE` | Yes | Yes | Yes |

Every public control request checks current account membership and the target's relation to the selected home. Core commands remain available to trusted application components. Authentication alone never gives access to every home.

The browser receives an opaque `HttpOnly; SameSite=Lax` cookie, also `Secure` on HTTPS. Only its SHA-256 digest is stored in Fluxzero's document store, allowing multiple app instances to share sessions. Expiry is capped by the ID token and eight hours; Fluxzero scheduling removes expired records. Logout deletes the session immediately. Requests and socket refreshes enforce expiry independently of cleanup delivery. No access or refresh token is stored in browser JavaScript.

Mutations require `X-Home-Request: 1` and reject a foreign `Origin`. WebSocket opens also reject foreign origins. This interface is same-origin; no permissive CORS configuration is added. Protected snapshots and authentication responses use `Cache-Control: no-store`.

For production, provision the OIDC client, exact HTTPS callback and initial account with the operator's Fluxzero identity setup. Configure these through the normal application property source; do not commit secrets:

| Property | Conventional environment name |
| --- | --- |
| `fluxzero.auth.external-base-url` | `FLUXZERO_AUTH_EXTERNAL_BASE_URL` |
| `fluxzero.auth.oidc.issuer` | `FLUXZERO_AUTH_OIDC_ISSUER` |
| `fluxzero.auth.oidc.client-id` | `FLUXZERO_AUTH_OIDC_CLIENT_ID` |
| `fluxzero.auth.oidc.redirect-uri` | `FLUXZERO_AUTH_OIDC_REDIRECT_URI` |
| `fluxzero.auth.oidc.resource-audience` | `FLUXZERO_AUTH_OIDC_RESOURCE_AUDIENCE` |
| `fluxzero.auth.oidc.scope` (default `openid profile email`) | `FLUXZERO_AUTH_OIDC_SCOPE` |
| `fluxzero.auth.oidc.login-state-secret` (shared random secret, at least 32 characters) | `FLUXZERO_AUTH_OIDC_LOGIN_STATE_SECRET` |
| `fluxzero.auth.oidc.token-endpoint-auth-method` (`none` or `private_key_jwt`) | `FLUXZERO_AUTH_OIDC_TOKEN_ENDPOINT_AUTH_METHOD` |
| `fluxzero.auth.oidc.client-private-jwk` (required for `private_key_jwt`) | `FLUXZERO_AUTH_OIDC_CLIENT_PRIVATE_JWK` |
| `fluxzero.auth.oidc.token-endpoint-audience` (optional override) | `FLUXZERO_AUTH_OIDC_TOKEN_ENDPOINT_AUDIENCE` |

Use `private_key_jwt` for a provisioned confidential client. The local managed IDP uses PKCE with `none`. A configured private key selects `private_key_jwt` unless an explicit method overrides it. Do not enable the local test IDP in a production deployment.

## HTTP and live updates

The generated reference is served at `/api/docs`, with OpenAPI at `/api/openapi.json`. Public discovery describes routes and schemas, not household data. The handlers and their annotations own this contract; there is no handwritten OpenAPI copy. JSON IDs are functional IDs, without their internal storage prefixes.

`POST /api/homes/{homeId}/spaces` accepts `{ "details": { "name": "Study", "kind": "ROOM" }, "enclosingSpaceId": null }` and returns `201` with `{ "spaceId": "..." }`. The server generates the identity. An optional enclosing space must belong to the selected home. The endpoint dispatches `AddSpace`, retaining the core details validation and parent model behavior.

HTTP adapters authorize the household, then dispatch existing Fluxzero commands or the `GetHomeOverview` query. They do not duplicate domain rules or call physical APIs. Device observations and desired settings are assembled from one pinned current Home graph.

Open a WebSocket at `/api/homes/{homeId}/live` using the same browser cookie. Each connection receives a complete `HomeOverview` snapshot initially and after committed changes in that Home graph. Later messages have that same shape. Each viewer has an SDK-owned endpoint instance, so one viewer does not replace another. Sending a text frame requests a fresh snapshot; the UI does this every 30 seconds and reconnects with bounded backoff. An invalid or revoked session closes the established view with code `1008` when checked. Reconnection re-reads account access. HTTP refresh is also used when the browser becomes visible again.

## Production assets and verification

CI installs dependencies from `frontend/package-lock.json`, runs `npm run check` (focused Node tests and a Vite production build), then packages the Java application. Maven copies `frontend/dist` into `classpath:/static`; `HomeFrontend` serves it through Fluxzero. Static serving excludes API, BFF and identity-provider paths and registers only when the built entry point exists. HTML is not given immutable caching.

For an explicit clean build outside an active development environment:

```bash
(cd frontend && npm ci && npm run check)
./mvnw -B verify
```

`HomeWebTest` exercises raw-cookie acquisition through the IDP stub, actual routed operations, household permissions, invalid inputs, logout and expiry, membership revocation, generated discovery, initial live snapshots and two simultaneous subscribers. The existing domain tests remain the deeper rules for atomic scenes and scheduling. Browser checks cover the real dev gateway, rendering and interactive flows; they do not imply physical hardware is connected.
