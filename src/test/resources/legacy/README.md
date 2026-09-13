# Historical Home fixtures

`rc11-commits.json` contains the ten pre-storage `CommitModels` requests emitted by Home commit `86933ce2276964e47142f676b26efaecfe148e99`, using the published SDK `2.0.0-rc.11`. They create a home, room, sensor, resident, zone, scene, routine and automation, then report 22 °C and 23 °C. The clock is fixed at `2026-09-14T10:00:00Z`.

These are original serialized event payloads and compressed Model documents, not current commands run through an upcaster before being stored. In particular, both `DeviceStatus` targets contain a direct `DeviceStatus` document **and** `storeEvent: true`. `rc11-status-document.json` captures the producer's resulting document and complete-history head.

`CaptureLegacy.java.txt` is the isolated producer. To reproduce, extract `src/main/java` from the pinned Home commit into a temporary directory, compile those historical sources (excluding `App`) with Java 25 and Lombok against rc.11 and its dependencies, then compile and run this producer against those classes. Do not run it against the current Home classes. Pass a temporary output directory; inspect the result before replacing these fixtures. The event transport `source` is normalized to `legacy-home-fixture` to omit machine identifiers. Payload bytes, document bytes, IDs, timestamps and read dependencies are retained. Capture the requests before calling the in-memory event store, which mutates event indices on acceptance.

`house-rev0.json` preserves the original development examples, with fully qualified historical type names and revision metadata. Its time-relative routine is omitted; the captured commits above contain an explicit historical routine deadline.

These fixtures belong to Home. They contain invented household data, with no devices, credentials or production records.
