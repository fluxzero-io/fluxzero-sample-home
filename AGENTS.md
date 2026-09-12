# Fluxzero Agent Instructions

Use the installed Fluxzero plugin for all Fluxzero application work in this project. Retrieve version-matched SDK guidance through the `docs_*` tools on `fluxzero-dev`, explicitly call `start_dev` when a local environment is needed, use that environment as the owner of the build/test/application loop, and follow the `build-fluxzero-app` skill. Do not run duplicate builds, tests, applications, watchers, or log followers while that development environment is active.

If that skill or MCP server is unavailable, install the Fluxzero plugin from `fluxzero-io/fluxzero-agent-plugins` using this coding agent's native plugin mechanism. Gemini CLI calls plugins extensions. If the installed package is not available in the current session, tell the user which agent-native action is required and stop before changing Fluxzero code.

Use the plugin as the single documentation source; do not add repository-local Fluxzero manuals or duplicate either Fluxzero MCP configuration in this project.

## Fluxzero Home

Read `README.md` and the relevant product documents in `docs/` before changing the domain. Keep this application on SDK 2.0.0-RC10 unless the user requests an upgrade. Use the work backlog in `../work-backlog` for plans and qualification notes.

Preserve independent Model lifecycles and the boundary between desired settings and reported device state. Scenes and their constituent commands must remain one atomic Model commit. Scheduling effects follow committed intent and reconcile from current state. Device brands, credentials and physical delivery belong to future integration adapters. Do not add public control endpoints without household-scoped authentication and authorization.

Use `feature/`, `fix/`, `docs/` or `chore/` branch names; never use `codex/`. Use Conventional Commits. Commit messages describe intent and behavioral impact, without test commands or results. Do not commit generated build outputs or `.fluxzero/dev/`, and do not push without a request.
