# Fluxzero Agent Instructions

Use the installed Fluxzero plugin for all Fluxzero application work in this project. Retrieve version-matched SDK guidance through the `docs_*` tools on `fluxzero-dev`, explicitly call `start_dev` when a local environment is needed, use that environment as the owner of the build/test/application loop, and follow the `build-fluxzero-app` skill. Do not run duplicate builds, tests, applications, watchers, or log followers while that development environment is active.

If that skill or MCP server is unavailable, install the Fluxzero plugin from `fluxzero-io/fluxzero-agent-plugins` using this coding agent's native plugin mechanism. Gemini CLI calls plugins extensions. If the installed package is not available in the current session, tell the user which agent-native action is required and stop before changing Fluxzero code.

Use the plugin as the single documentation source; do not add repository-local Fluxzero manuals or duplicate either Fluxzero MCP configuration in this project.

## Fluxzero Home

Read `README.md` and the relevant product documents in `docs/` before changing the domain. Keep this application on local SDK `2.0.0-rc.11-local.d88696f27d26`, built from exact commit `d88696f27d26c03c29785c6fbf1c32cee270ae67`, unless the user requests an upgrade. `scripts/prepare-sdk.sh` installs this unpublished candidate from the adjacent SDK repository without modifying that checkout. Use the work backlog in `../work-backlog` for plans and qualification notes.

For uncached candidate docs, start the plugin MCP process with `FLUXZERO_DEV_DOCS_SDK_ARCHIVE` pointing to `~/.m2/repository/io/fluxzero/fluxzero-sdk-java/2.0.0-rc.11-local.d88696f27d26/fluxzero-sdk-java-2.0.0-rc.11-local.d88696f27d26-agent-docs.zip` (expand to an absolute path). Select the exact version through `docs_start` and verify its source commit. The normal shared cache remains usable afterwards; do not silently substitute rc.11 manuals.

Preserve independent Model lifecycles and the boundary between desired settings and reported device state. Scenes and their constituent commands must remain one atomic Model commit. Scheduling effects follow committed intent and reconcile from current state. Device brands, credentials and physical delivery belong to future integration adapters. Do not add public control endpoints without household-scoped authentication and authorization.

The existing home-scoped queries use component documents from explicit relationship paths. Keep Models on plain `@Model`; justify additional persistence against an actual query or authoritative load requirement. `DeviceStatus` requires event sourcing for event-bound before/after readings; do not add a duplicate previous-reading field. Routine schedule reconciliation uses one tracker and receives cascade changes through its sole-Graph handler. See `docs/sdk-2.md` for this application's choices and the versioned SDK query guide for the full contract.

Use `feature/`, `fix/`, `docs/` or `chore/` branch names; never use `codex/`. Use Conventional Commits. Commit messages describe intent and behavioral impact, without test commands or results. Do not commit generated build outputs or `.fluxzero/dev/`, and do not push without a request.

Use domain-specific details values for descriptive input and preserve focused command intent. This example app has not been deployed and uses the current schema without upcasters or explicit serialization revisions. Use a fresh temporary runtime for incompatible local example data. Add schema migrations only when there is actual historical data to preserve or the user explicitly requests a migration example. Model/event revisions and routine generations used for ordering, deduplication and scheduling are separate domain behavior and remain required.
