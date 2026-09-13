# SDK 2.0 in dit voorbeeld

De build importeert `io.fluxzero:fluxzero-bom:2.0.0-rc.11`. De Maven Compiler voert ook de bijpassende SDK-annotationprocessor uit, zodat model- en type-indexen worden gegenereerd. De ontwikkelomgeving gebruikt de bijpassende runtime voor SDK rc.11.

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
| Doelgerichte opslag | Acht Models gebruiken gewone `@Model`: hun eventstream blijft leidend. Alleen `DeviceStatus` gebruikt `DOCUMENT` als actuele bron. |
| Relatiebewust zoeken | `FindDevices` selecteert via `whereAncestor(homeId)` en de gevraagde mogelijkheid. |
| Alternatieve identiteit | Een optioneel apparaatlabel is een `@Alias`; de echte apparaat-ID blijft stabiel. |
| Actuele en eventgebonden Graphs | Consumers kunnen de toestand bij een verandering lezen; schedule-reconciliatie kiest juist expliciet de actuele routine. |
| Versioned defaults | `2026.09.10` kiest de 2.0-defaults voor conflictherhaling en automatische routing. |
| Deterministische scheduling en TestFixture | Deadlines, generaties, pauzeren, herhaling, klokovergangen en stale delivery zijn gedragstests. |

De applicatie gebruikt geen legacy Aggregates voor nieuwe toestand. Scènestappen zijn waardes zonder zelfstandige levenscyclus; daarom wordt `@Member` niet alleen voor een featuredemonstratie toegevoegd. Een volledige huisprojectie wordt niet bij iedere sensorwaarde gematerialiseerd: dat zou onnodig werk opleveren.

`Graph.revisionStateIndex()` is de revisie van één node, niet noodzakelijk dezelfde waarde voor iedere node in een atomair commit met meerdere events. De scènetest controleert daarom de complete duurzaam gecommitteerde toestand via een expliciet actuele Graph tijdens eventafhandeling, en rollback na een later geweigerde deelhandeling. Een eventgebonden Graph blijft bewust de historische grens van dat afzonderlijke event tonen.

## Opslag en zoeken in dit huis

`Home`, `Space`, `Device`, `Zone`, `Resident`, `Scene`, `Routine` en `Automation` gebruiken gewone `@Model`. Hun eigen eventstream blijft de bron voor laden en herladen. `DeviceStatus` gebruikt `@Model(persistence = DOCUMENT)`: daar is het laatste volledige rapport de actuele bron. Die keuze staat los van het publiceren van veranderingen.

| Vraag in de app | Gebruikte route | Waarom deze opslag volstaat |
| --- | --- | --- |
| Hoe is dit bekende huis ingedeeld? | `GetHome` laadt `Graph<Home>` via HomeId. | Laden via identiteit en navigeren door relaties vereisen geen directe documentprojectie op `Home`. |
| Welke apparaten in dit huis kunnen dimmen? | `FindDevices` zoekt `Device` via `whereAncestor(homeId)` en filtert op `capabilities`. | Het bestaande `devices`-compositiepad onderhoudt geïndexeerde interne apparaatdocumenten. Het bekende huis heeft hiervoor geen eigen document nodig. |
| Welke automatiseringen horen bij deze verandering? | `HomeReactions` zoekt via `whereParent(homeId)`. | Het bestaande `automations`-compositiepad onderhoudt de interne documenten voor deze begrensde selectie. |
| Wat heeft het apparaat werkelijk gemeld? | `GetDeviceStatus` laadt via DeviceStatusId. | Alleen hier is het actuele document de gekozen bron voor de Modelwaarde. |

`Fluxzero.search(Device.class)` zonder huisselectie geeft met deze configuratie geen apparaten terug. Een globale apparatenlijst is geen huidige applicatiequery. Daarvoor zou een expliciete directe documentprojectie een nieuwe, te onderbouwen keuze zijn. Zoekzichtbaarheid is geen toegangscontrole.

`GetHome` gebruikt een via identiteit geladen relatie-Graph. Dat is een andere route dan `searchGraph(Home.class)`, waarvoor de huidige kale huisroot geen document heeft. Een doorzoekbare projectie van complete huizen wordt pas toegevoegd wanneer een concrete vraag die nodig heeft; de app materialiseert niet bij elke sensormeting een heel huis.

Zoekresultaten tonen gecommitteerde actuele documenten en zijn geen historische eventtoestand of transactionele leesafhankelijkheden. Domeinregels blijven daarom de geïnjecteerde Models/Graphs gebruiken. De scheduler vraagt met `loadCurrentGraph` bewust naar de actuele routine; rc.11 legt daarvoor tijdens de aanroep een verse opslaggrens vast, ook wanneer de root al in de cache staat.

De [rc.11-releasebeschrijving](https://github.com/fluxzero-io/fluxzero-sdk-java/releases/tag/2.0.0-rc.11) bevat de verduidelijkte querycontracten en het herstel voor expliciet actuele Graphreads. De correctie voor bestaande ouderrelaties uit RC10 blijft in dit voorbeeld zichtbaar. Voor de complete mogelijkhedenmatrix is de [versiegebonden querykeuzehulp](https://github.com/fluxzero-io/fluxzero-sdk-java/blob/2.0.0-rc.11/docs/developer/guides/Modeling%20%26%20persistence/205-model-query-guide.mdx) leidend; deze app beschrijft alleen haar eigen keuzes.
