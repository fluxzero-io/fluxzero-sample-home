# SDK 2.0 in dit voorbeeld

De build importeert `io.fluxzero:fluxzero-bom:2.0.0-RC10`. De Maven Compiler voert ook de bijpassende SDK-annotationprocessor uit, zodat model- en type-indexen worden gegenereerd. De ontwikkelomgeving gebruikt de bijpassende RC10-runtime.

| SDK-mogelijkheid | Concrete toepassing |
| --- | --- |
| Zelfstandige `@Model`-grenzen | Huis, ruimte, apparaat, bewoner, zone, scène, routine en automatisering hebben hun eigen levenscyclus. |
| Getypeerde `Id<T>` | De naamruimte van iedere soort ID voorkomt botsingen tussen bijvoorbeeld een kamer en lamp met dezelfde naam. |
| Automatische commandafhandeling | Commands dragen hun eigen `@Apply`; er zijn geen doorgeefhandlers met handmatig laden en opslaan. |
| `@Parent` en recursieve relaties | Ruimtes vormen een vrije boom; apparaten blijven zelfstandige kinderen van hun ruimte. |
| Luie, consistente `Graph<T>` | Huisgrenzen, zoneselectie, scènedoelen en verwijdervoorwaarden lezen alleen de benodigde relaties. |
| Interceptie en atomaire meerdere Models | `ActivateScene` breidt de bedoeling uit tot gewone apparaatcommands en de activatie zelf binnen één commit. |
| RC10: schrijven naar bestaande ouders | `RemoveDevice` wist een apparaat én corrigeert het primaire licht van de bestaande kamer, zonder haar ID in het command. `@Association("devices")` onderscheidt de kamer van bovenliggende ruimtes. |
| Legale en recursieve assertions | Huisgrenzen, mogelijkheden en instellingswaarden worden vóór uitvoering gecontroleerd. `DefineScene` geeft instellingsvalidators terug. |
| Event sourcing én actuele documenten | De meeste Models bewaren geschiedenis én een doorzoekbare actuele weergave. `DeviceStatus` is bewust een actueel DOCUMENT-model. |
| Relatiebewust zoeken | `FindDevices` selecteert via `whereAncestor(homeId)` en de gevraagde mogelijkheid. |
| Alternatieve identiteit | Een optioneel apparaatlabel is een `@Alias`; de echte apparaat-ID blijft stabiel. |
| Actuele en eventgebonden Graphs | Consumers kunnen de toestand bij een verandering lezen; schedule-reconciliatie kiest juist expliciet de actuele routine. |
| Versioned defaults | `2026.09.10` kiest de 2.0-defaults voor conflictherhaling en automatische routing. |
| Deterministische scheduling en TestFixture | Deadlines, generaties, pauzeren, herhaling, klokovergangen en stale delivery zijn gedragstests. |

De applicatie gebruikt geen legacy Aggregates voor nieuwe toestand. Scènestappen zijn waardes zonder zelfstandige levenscyclus; daarom wordt `@Member` niet alleen voor een featuredemonstratie toegevoegd. Een volledige huisprojectie wordt niet bij iedere sensorwaarde gematerialiseerd: dat zou onnodig werk opleveren.

`Graph.revisionStateIndex()` is de revisie van één node, niet noodzakelijk dezelfde waarde voor iedere node in een atomair commit met meerdere events. De scènetest controleert daarom de complete duurzaam gecommitteerde toestand via een expliciet actuele Graph tijdens eventafhandeling, en rollback na een later geweigerde deelhandeling. Een eventgebonden Graph blijft bewust de historische grens van dat afzonderlijke event tonen.

De [RC10-releasebeschrijving](https://github.com/fluxzero-io/fluxzero-sdk-java/releases/tag/2.0.0-RC10) beschrijft de reparatie voor bestaande ouderrelaties. De volledige versiegebonden SDK-documentatie is via de Fluxzero-plugin beschikbaar; deze repository dupliceert die handleiding niet.
