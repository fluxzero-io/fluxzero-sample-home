# SDK 2.0 in dit voorbeeld

De build importeert `io.fluxzero:fluxzero-bom:2.0.0-rc.11-local.9ae3f349a2a`, een lokale build van SDK-commit `9ae3f349a2a94f9353e0e8419dd2ace299cc9fd7`. De Maven Compiler voert ook de bijpassende SDK-annotationprocessor uit, zodat model- en type-indexen worden gegenereerd. `scripts/prepare-sdk.sh` bouwt de bron van deze commit met een eigen versienaam; er wordt niets gepubliceerd en de gepubliceerde rc.11 wordt niet vervangen.

| SDK-mogelijkheid | Concrete toepassing |
| --- | --- |
| Zelfstandige `@Model`-grenzen | Huis, ruimte, apparaat, bewoner, zone, scène, routine en automatisering hebben hun eigen levenscyclus. |
| Cohesieve details | Acht details-value-objects met gevalideerde commandinvoer en gerichte wijzigingen die andere gegevens behouden. |
| Getypeerde `Id<T>` | De naamruimte van iedere soort ID voorkomt botsingen tussen bijvoorbeeld een kamer en lamp met dezelfde naam. |
| Automatische commandafhandeling | Commands dragen hun eigen `@Apply`. Alleen de twee uitvoeringscommands hebben een expliciete handler om functionele afwijzing als pauze af te handelen. |
| `@Parent` en recursieve relaties | `Space.parentId` kiest één Home of Space via `PlaceId<?>`; de gedeclareerde parenttypes en het pad `spaces` behouden de vrije boom. |
| Luie, consistente `Graph<T>` | Huisgrenzen, zoneselectie, scènedoelen en verwijdervoorwaarden lezen alleen de benodigde relaties. |
| Interceptie en atomaire meerdere Models | `ActivateScene` breidt de bedoeling uit tot gewone apparaatcommands en de activatie zelf binnen één commit. |
| RC10: schrijven naar bestaande ouders | `RemoveDevice` wist een apparaat én corrigeert het primaire licht van de bestaande kamer, zonder haar ID in het command. `@Association("devices")` onderscheidt de kamer van bovenliggende ruimtes. |
| Invoer vóór Modelcontroles | Jakarta-constraints en pure `@AssertTrue`-methoden controleren namen, rollen, capabilities, cooldowns, triggers, tijdpatronen en meetwaarden. `@Valid` neemt de concrete geneste waarden mee. `@AssertLegal` bewaakt daarna model- en relatieafhankelijke regels. |
| Gedelegeerde Modelassertions | `@AssertLegal` op `DefineAutomation.trigger` laat de concrete `MeasurementCrosses` zelf haar sensor en meetmogelijkheid controleren via de gepinde huis-Graph. |
| Expliciete eventpublicatie | `ActivateScene` gebruikt `@Apply(eventPublication = ALWAYS)` en retourneert de bestaande scène: activatiehistorie zonder gekopieerde auditvelden. |
| Doelgerichte opslag en historie | Alle negen Models gebruiken gewone `@Model`. Ook apparaatwaarnemingen hebben historie nodig voor het herkennen van grensoverschrijdingen. |
| Creationcompatibiliteit en relaties | De SDK weigert dubbele creatie; `AddSpace` vereist één bestaande bestemming. `MoveSpace` bewaakt het oorspronkelijke huis. |
| Complete Graph-change-handlers | `RoutineSchedules` reconcilieert status en deadlines vanuit de actuele routine, ook na een ouder event. |
| Schedules met `@Parent` | `RunRoutine.routineId` koppelt een uitvoering aan de levensduur van haar Routine. Directe en cascaderende verwijdering annuleren opgeslagen uitvoeringen zonder applicatiecleanup. |
| Relatiebewust zoeken | `FindDevices` selecteert via `whereAncestor(homeId)` en de gevraagde mogelijkheid. |
| Eén identiteit en parent | `DeviceStatus.deviceId` draagt `@EntityId(prefix = "devicestatus:")` én `@Parent`; rapporteren vereist alleen de apparaat-ID. |
| Alternatieve identiteit | Een optioneel apparaatlabel is een `@Alias`; de echte apparaat-ID blijft stabiel. |
| Actuele en eventgebonden Graphs | Consumers kunnen de toestand bij een verandering lezen; schedule-reconciliatie kiest juist expliciet de actuele routine. |
| Versioned defaults | `2026.09.10` kiest de 2.0-defaults voor conflictherhaling en automatische routing. |
| Deterministische scheduling en TestFixture | Deadlines, generaties, pauzeren, herhaling, klokovergangen en stale delivery zijn gedragstests. |

De routinepatronen `Once` en `Weekly` zijn concrete waarden achter het `RoutineTiming`-contract. Zij krijgen een expliciet referentietijdstip en de huistijdzone; de app gebruikt hiervoor geen systeemklok. `@Valid` op `PlanRoutine.timing` neemt de veldconstraints van het gekozen patroon mee. `Weekly` bezit de lokale kalenderberekening, terwijl de bestaande routineconsumer de berekende deadline met de SDK-scheduler synchroniseert. De polymorfe JSON-namen blijven `once` en `weekly`.

Een ongeldige invoerwaarde of leeg tijdpatroon wordt als `ValidationException` met een veldpad afgewezen, ook wanneer het opgegeven huis nog niet bestaat. `AddDevice` laadt geen ruimte voor de vraag of de invoer mogelijkheden of metingen bevat. Zijn `@Apply(Space)` bewaakt via de SDK nog steeds dat het apparaat in een bestaande ruimte wordt aangemaakt. Bij gedeserialiseerde invoer verzorgt Fluxzero de standaardwaarden voor collecties; de commands voegen daarvoor geen constructors toe.

Veldconstraints gaan vóór methodconstraints. Daarom controleert bijvoorbeeld `hasNonNegativeCooldown()` alleen `!cooldown.isNegative()`: `@NotNull` op het veld heeft ontbrekende invoer al afgevangen. Hetzelfde geldt voor verplichte collectie-elementen. Checks op bewust optionele gegevens, zoals een apparaatlabel of media bij gestopte weergave, beschrijven wel een eigen domeinregel.

Bij automatiseringen cascadeert `@Valid` de invoerconstraints van de concrete trigger; `@AssertLegal` delegeert daarna de Modelcontroles. `DefineAutomation` bewaakt de huisgrens van de definitie en de scène, zonder de triggerimplementaties te kennen. `HomeBecomes` en `MeasurementCrosses` bezitten hun eigen overgangsdetectie. `HomeChange` onderscheidt een moduswijziging van een apparaatwaarneming met concrete waarden; beide interfaces bevatten alleen contracten. De zoekselectie gebruikt dezelfde overgangsregel als de uiteindelijke commandafhandeling, die altijd de geladen automatiseringsdefinitie controleert.

`ReportDeviceStatus` vergelijkt de waarnemingstijd met de oorspronkelijke publicatietijd van het bericht. Deze contextafhankelijke controle blijft op de vastgelegde kandidaat een `@AssertLegal(Message)` zonder Modelparameter. `@PastOrPresent` ten opzichte van de verwerkingsklok zou een andere tijdgrens kiezen. De ontbrekende waarnemingstijd, apparaatidentiteit, bereikbaarheid en meetwaarden worden wel vooraf declaratief gevalideerd.

De applicatie gebruikt geen legacy Aggregates voor nieuwe toestand. Scènestappen zijn waardes zonder zelfstandige levenscyclus; daarom wordt `@Member` niet alleen voor een featuredemonstratie toegevoegd. Een volledige huisprojectie wordt niet bij iedere sensorwaarde gematerialiseerd: dat zou onnodig werk opleveren.

`Graph.revisionStateIndex()` is de revisie van één node, niet noodzakelijk dezelfde waarde voor iedere node in een atomair commit met meerdere events. De scènetest controleert daarom de complete duurzaam gecommitteerde toestand via een expliciet actuele Graph tijdens eventafhandeling, en rollback na een later geweigerde deelhandeling. Een eventgebonden Graph blijft bewust de historische grens van dat afzonderlijke event tonen.

## Opslag en zoeken in dit huis

`Home`, `Space`, `Device`, `Zone`, `Resident`, `Scene`, `Routine`, `Automation` en `DeviceStatus` gebruiken gewone `@Model`. Hun eigen eventstream blijft de bron voor laden en herladen. `DeviceStatus` bevat alleen het huidige rapport; de eventgebonden Graph levert de vorige waarneming via `previous()`. Die vergelijking blijft beschikbaar na cachewissen en bij latere rapporten, zonder een dubbel opgeslagen `previousReadings`-veld.

| Vraag in de app | Gebruikte route | Waarom deze opslag volstaat |
| --- | --- | --- |
| Hoe is dit bekende huis ingedeeld? | `GetHome` laadt `Graph<Home>` via HomeId. | Laden via identiteit en navigeren door relaties vereisen geen directe documentprojectie op `Home`. |
| Welke apparaten in dit huis kunnen dimmen? | `FindDevices` zoekt `Device` via `whereAncestor(homeId)` en filtert op `capabilities`. | Het bestaande `devices`-compositiepad onderhoudt geïndexeerde interne apparaatdocumenten. Het bekende huis heeft hiervoor geen eigen document nodig. |
| Welke automatiseringen horen bij deze verandering? | `HomeReactions` zoekt via `whereParent(homeId)`. | Het bestaande `automations`-compositiepad onderhoudt de interne documenten voor deze begrensde selectie. |
| Wat heeft het apparaat werkelijk gemeld? | `GetDeviceStatus` laadt met `loadModel(deviceId, DeviceStatus.class)`. | De eigen waarnemingsgeschiedenis reconstrueert het actuele rapport; de relatie naar het apparaat blijft gelijk. |

`Fluxzero.search(Device.class)` zonder huisselectie geeft met deze configuratie geen apparaten terug. Een globale apparatenlijst is geen huidige applicatiequery. Daarvoor zou een expliciete directe documentprojectie een nieuwe, te onderbouwen keuze zijn. Zoekzichtbaarheid is geen toegangscontrole.

`GetHome` gebruikt een via identiteit geladen relatie-Graph. Dat is een andere route dan `searchGraph(Home.class)`, waarvoor de huidige kale huisroot geen document heeft. Een doorzoekbare projectie van complete huizen wordt pas toegevoegd wanneer een concrete vraag die nodig heeft; de app materialiseert niet bij elke sensormeting een heel huis.

Zoekresultaten tonen gecommitteerde actuele documenten en zijn geen historische eventtoestand of transactionele leesafhankelijkheden. Domeinregels blijven daarom de geïnjecteerde Models/Graphs gebruiken. De scheduler vraagt met `loadCurrentGraph` bewust naar de actuele routine; rc.11 legt daarvoor tijdens de aanroep een verse opslaggrens vast, ook wanneer de root al in de cache staat.

De querycontracten van rc.11 blijven behouden. De lokale kandidaat voegt lifecycleafwijzingen, nullable leesreferenties, revisiebehoud bij dynamische schrijftargets en cascaderende Graph-notificaties toe. De bijbehorende agentdocumentatie zit in het lokaal geïnstalleerde `agent-docs`-archief met dezelfde versie en broncommit; `AGENTS.md` beschrijft het laden via de Fluxzero-plugin.

## Keuzes na het Modelcontractherstel

Een scène blijft uit gewone apparaatcommands bestaan. Dat geeft herkenbare gebeurtenissen en laat ieder command zijn eigen regels toepassen. `SceneAction` en `SceneTarget` zijn contracten zonder implementaties. Concrete acties maken de commands en concrete selecties lezen hun deel van de huis-Graph. De app bevat geen eigen Model-simulator of alternatieve route met dynamische Models om het SDK-contract opnieuw te testen.

Dubbele `CreateHome`, `AddSpace`, `AddDevice` en `AddResident` worden door de SDK functioneel afgewezen. Daarvoor staan geen extra `requireNew`-assertions meer in de app. Commands die herdefinitie bedoelen, zoals `DefineScene` en `PlanRoutine`, hebben expliciet een nullable huidige Modelparameter. De app bewaakt nog steeds haar eigen businessregels. Invoer gebruikt declaratieve constraints; relationele domeinafwijzingen gebruiken rechtstreeks `IllegalCommandException`. Er is geen algemene Rules-helper of eigen exception-wrapper. `PlanRoutine` benoemt huisgrens, scènekeuze en toekomstige uitvoering in afzonderlijke assertions.

`Home` en `Space` implementeren het zuivere `Place`-contract met alleen `id()`. `HomeId` en `SpaceId` vormen de gesloten `PlaceId`-familie. `Space.parentId` declareert beide concrete Models als `@Parent(types = {Home.class, Space.class}, pathInParent = "spaces")`; het bijbehorende huis wordt niet dubbel opgeslagen. `MoveSpace` leest de Graph van de bestaande ruimte en zoekt de bestemming binnen haar oorspronkelijke huis, inclusief de huisroot zelf.

Op deze SDK-pin is `Place` geen zelfstandig `@Model`: injectie van het gedeelde interfacetype botst met de concrete types in de Modelcache. Daarom controleert `AddSpace` de verplichte ouder in een kleine `@AssertLegal` via `Fluxzero.loadModel(parentId)`, dat het concrete ID-type gebruikt. De apply maakt alleen de ruimte. Er is geen eigen resolver of uitvoeringslaag.

Een polymorfe `parentId` gebruikt JSON `["home", "example-home"]` of `["space", "example-floor"]`. Gewone `HomeId`- en `SpaceId`-velden blijven scalars. `PlaceId` laat Jackson de concrete subtypeconstructor kiezen met type-info en `@JsonCreator`; de standaard ID-deserializer van de gepinde SDK probeert anders de abstracte ID-basis zelf te construeren.

`ReactToHome` en `RunRoutine` hebben een kleine `@HandleCommand` die `Fluxzero.assertAndApply(this)` uitvoert. Dat doorloopt de volledige Modelcommit zonder het command opnieuw te versturen. Hun `@InterceptApply` retourneert de scène-activatie en de voortgang als één samengestelde bedoeling. Er is geen voorafgaande scèneproef: controles en applies horen bij dezelfde commitpoging.

Bij een `FunctionalException` is die poging teruggedraaid en volgt een apart `PauseFailedRoutine`- of `PauseFailedAutomation`-command. Technische fouten worden doorgelaten. Succesvolle apparaatinstellingen en workflowvoortgang blijven één commit; afwijzing en het daaropvolgende pauzeren zijn twee transacties. De app claimt daarvoor geen crashbestendige workflowgarantie. Het pauzecommand controleert opnieuw de routinegeneratie/deadline of de toepasselijkheid van de automatiseringsdefinitie.

Routine en Automation hebben geen uitvoertellers of gekopieerde laatste uitvoerdatum. `ReactToHome` publiceert succesvolle reacties expliciet met `eventPublication = ALWAYS`, ook als nul cooldown en hetzelfde tijdstip geen nieuwe waarde opleveren. `Automation.cooldownEndsAt` bewaart de actuele eindgrens van de rustperiode. `effectiveFrom` is het begin van de huidige definitie. De rustregel heeft daarmee constante kosten en loopt niet bij iedere sensorwaarde door de historie.

De routineconsumer reconcilieert de actuele toestand met één tracker, ook na een historisch event. Zijn sole-Graph-handler ontvangt zowel directe wijzigingen als cascadeverwijdering. Voor een afwezige routine doet de consumer niets: `@Parent` op `RunRoutine.routineId` laat de SDK de opgeslagen uitvoering annuleren. Pauzeren, afronden en herplannen blijven expliciete schedule-effecten van de actuele routine. Deadlines en generaties beschermen nog steeds tegen oude of al afgeleverde opdrachten.

De parent moet al gecommit zijn wanneer de uitvoering wordt gepland; de bestaande post-commit consumer voldoet daaraan. Annulering is asynchroon en werkt ook zonder actieve applicatieconsumer. De lokale TestServer hoort bij dezelfde SDK-kandidaat. Een aparte gedeployde Runtime moet deze ownershipfunctie ondersteunen. `RoutineOwnershipTest` bewijst cleanup zonder routineconsumer en dat hercreatie met hetzelfde ID een oude opgeslagen uitvoering niet opnieuw geldig maakt.

## Voorbeelddata en publicatie

Deze app is nog niet uitgerold en begint met het huidige details-schema. Er zijn geen upcasters, expliciete schemarevisies of migratiefixtures nodig. Gebruik bij een onverenigbare schemawijziging een nieuwe tijdelijke runtime voor het voorbeeldhuis.

Gewone event-sourced replay en historische waarnemingen blijven onderdeel van het domein. Automatiseringen vergelijken eventgebonden vóór/na-toestand, zonder eigen bronrevisies voor deduplicatie of een eis dat de bron sindsdien ongewijzigd is gebleven. De gedragstests controleren triggers, rustperiodes en pauzeren; technische herafleveringsgaranties behoren bij SDK en Runtime. Volledige durable execution is nog geen garantie van de vastgelegde kandidaat. Routinegeneraties blijven de identiteit van een herplanning onderscheiden en zijn geen schemarevisies. Herladen, vorige metingen en schedulecleanup blijven afgedekt. Retentie van waarnemingen blijft een afzonderlijke productkeuze.

De kandidaat is geen openbare SDK-release. Lokale builds gebruiken de meegegeven SDK-repository. CI en deployment moeten dezelfde commit uit de SDK-repository kunnen ophalen. Vervang de lokale versie pas door een gepubliceerde SDK-versie die deze commit bevat.

## Home Assistant: gecommitteerde wensen en externe waarnemingen

`HomeAssistantConnection` is een Model onder Home. `HomeAssistantDevice` heeft zowel Device als verbinding als parent: zij houdt op te bestaan zodra één van beide verdwijnt. Het apparaat wordt niet verwijderd wanneer zijn verbinding verdwijnt. De samengestelde entity-alias bevat de verbindingsidentiteit en voorkomt dubbele koppeling binnen die installatie. Beide Models blijven gewone event-sourced Models.

`LinkHomeAssistantDevice` heeft een eigen `@TrackSelf`-commandhandler: hij haalt de actuele API-mogelijkheden op en past daarna één Modelcommand toe. De gateway wordt via `@Autowired` als handlerparameter geleverd; de fixture gebruikt daarvoor `withBean`. De checks voor huisgrenzen en modelbestaan blijven in de Modelpipeline.

De post-commit eventconsumer levert apparaatinstellingen af. Een herpoging bevat alleen apparaat en verbinding en leest met `loadCurrentGraph` de actuele wens. Door de Device-Graph naar haar gekoppelde Model en status te navigeren blijft ook een nog afwezige of verwijderde koppeling een normale lege relatie.

`RefreshHomeAssistant` en `DeliverHomeAssistantSettings` tonen schedule-ownership voor externe systemen. Een refresh produceert complete, gewijzigde `ReportDeviceStatus`-waarnemingen, zodat de bestaande eventgebonden automatiseringen zonder leverancierslogica blijven werken. `AcceptHomeAssistantObservation` controleert de nog gekozen route vóór het rapport in dezelfde Modelpipeline wordt toegepast. Zie de [integratiehandleiding](home-assistant.md) voor fysieke aflevering, polling en de grenzen van deze SDK-kandidaat.
