# Scènes en tijd

## Een scène als één bedoeling

Een scène bevat geordende, concrete acties zoals `DimLights`, `SetHeating` en `SwitchPower`. Een actie richt zich op één apparaat, op een ruimte en haar onderliggende ruimtes, op een zone of op het hele huis.

Bij één specifiek apparaat moet dat apparaat de instelling ondersteunen. Bij een grotere doelgroep worden alleen apparaten met de gevraagde mogelijkheid geselecteerd. Als er geen geschikt apparaat is, wordt de hele scène geweigerd. Zones met overlappende ruimtes passen dezelfde uiteindelijke instelling maar één keer toe. Een latere actie mag een eerdere instelling verfijnen. Ongeldige acties worden ook geweigerd wanneer een latere actie ze zou overschrijven.

De selectie wordt opnieuw gemaakt bij iedere activatie. Daardoor hoort een lamp na verplaatsing bij haar nieuwe kamer. Alle controles en wijzigingen gebeuren op één vastgelegde Graph-toestand. Fluxzero voert de herkenbare deelhandelingen en de scène-activatie uit in één Model-transactie. Andere applicatiecomponenten krijgen geen half uitgevoerde scène te zien.

De selecties `OneDevice`, `InSpace`, `InZone` en `WholeHome` bepalen welke apparaten meedoen. De concrete scèneactie maakt per apparaat een herkenbaar command. `ActivateScene` kiest per apparaat en mogelijkheid de laatste actie en laat de SDK die commands gezamenlijk toepassen. Er worden geen tijdelijke apparaatmodellen opgebouwd om daar achteraf commands uit af te leiden.

Iedere geslaagde activatie wordt gepubliceerd en in de Model-historie vastgelegd met `@Apply(eventPublication = ALWAYS)`. `Scene` bewaart hiervoor geen teller of tijdstempel. Als de apparaten al goed staan, blijft alleen de activatie over als nieuw event.

Dat is een garantie over de kern. Latere fysieke apparaten kunnen afzonderlijk bereikbaar zijn of falen; adapters zullen die voortgang afzonderlijk terugmelden.

## Een routine heeft een blijvende volgende uitvoering

`PlanRoutine` legt zowel het patroon als de concrete volgende uitvoering vast. Een post-commit consumer brengt de scheduler in overeenstemming met de actuele routine. Een opnieuw afgeleverd oud event kan daardoor geen gepauzeerde routine opnieuw plannen.

```java
new PlanRoutine(appointment, home, new RoutineDetails("Eenmalig comfort"),
        evening, new Once(moment));

new PlanRoutine(weekRhythm, home, new RoutineDetails("Vaste avonden"),
        evening, new Weekly(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), LocalTime.of(20, 0)));
```

`RoutineTiming` bevat uitsluitend het contract voor de volgende uitvoering. `Once` bewaart één absoluut tijdstip en is daarna klaar. `Weekly` kiest zelf de eerstvolgende gekozen weekdag en lokale tijd in de tijdzone van het huis. Die kalenderregel staat bij het concrete patroon. Beide patronen zoeken strikt ná het opgegeven moment; een eenmalige afspraak voor precies nu wordt bij het plannen afgewezen. Invoerconstraints staan op de concrete waarden.

Elke routine heeft één stabiele schedule-identiteit en een oplopende generatie. Een uitvoering controleert generatie, deadline en pauzestand. Vroege, dubbele of verouderde afleveringen veranderen niets. De scène en het afronden of doorschuiven van de routine worden samen gecommit.

- Een eenmalige routine eindigt na uitvoering.
- Een wekelijkse routine kiest de eerstvolgende toekomstige lokale datum en tijd.
- Een niet-bestaand tijdstip bij de overgang naar zomertijd wordt overgeslagen.
- Een dubbel tijdstip bij wintertijd wordt uitsluitend bij de eerste gelegenheid uitgevoerd.
- Een late aflevering voert één keer uit en slaat gemiste herhalingen over.
- Hervatten start bij de volgende toekomstige gelegenheid. Een verstreken eenmalige afspraak moet opnieuw worden gepland.
- Herplannen vervangt het actieve tijdstip en maakt de vorige generatie ongeldig.
- Pauzeren annuleert de volgende uitvoering via de routineconsumer.
- `RunRoutine` verwijst met `@Parent` naar haar routine. De SDK annuleert opgeslagen uitvoeringen bij verwijdering van die routine, ook wanneer haar huis cascaderend wordt verwijderd en de routineconsumer niet actief is.

Ook een nieuwe planning nadat de eerste gelegenheid van een dubbel lokaal tijdstip voorbij is, slaat de tweede over en kiest de volgende passende datum. De opgeslagen JSON-patronen behouden de namen `once` en `weekly`.

Deze automatische annulering is asynchroon en trekt reeds afgeleverde opdrachten niet terug. Daarom blijven de controles op actuele toestand, generatie en deadline nodig. Een nieuwe routine met hetzelfde ID krijgt een nieuwe levensduur: een eerder opgeslagen uitvoering kan niet opnieuw aan die nieuwe routine worden gekoppeld.

Als een scène door een gewijzigde huisinrichting niet meer uitgevoerd kan worden, wordt de routine gepauzeerd met een begrijpelijke reden in `problem`. Geen van de apparaatinstellingen wordt dan gedeeltelijk toegepast. Na herstel kan een herhalende routine hervat worden; een gemiste eenmalige routine kan opnieuw worden gepland.

## Reageren op veranderingen

Een automatisering kan reageren wanneer het huis een bepaalde modus krijgt, of wanneer een sensorwaarde een grens omhoog of omlaag passeert. Een eerste sensorwaarde bewijst nog geen grensovergang. Waarden die aan dezelfde kant van de grens blijven, activeren de scène niet opnieuw.

```java
new DefineAutomation(leaving, home, new AutomationDetails("Bij vertrek"),
        everythingOff, new HomeBecomes(HomeMode.AWAY), Duration.ZERO);

new DefineAutomation(tooWarm, home, new AutomationDetails("Te warm"),
        cooling, new MeasurementCrosses(sensor, Measurement.TEMPERATURE,
                MeasurementCrosses.Direction.RISES_ABOVE, new BigDecimal("24")),
        Duration.ofMinutes(5));
```

`HomeBecomes` herkent het binnengaan van de gekozen modus. `MeasurementCrosses` herkent een strikte grenspassage door de gekozen sensor: van 24 naar 25 telt bij *boven 24*, van 23 naar 24 nog niet. Als een meting in een van beide rapporten ontbreekt, is er geen bewezen overgang. De sensor moet in hetzelfde huis staan en de gekozen meting leveren; de sensortrigger bewaakt die voorwaarden bij het definiëren van de automatisering.

De concrete triggers bevatten hun eigen regels. `AutomationTrigger` is uitsluitend het contract. De reactieconsumer maakt een `HomeModeChanged` of `DeviceObservationChanged` en selecteert met diezelfde triggerregel de passende automatiseringen. `ReactToHome` toetst de verandering opnieuw tegen de dan geldende definitie en rustperiode voordat de scène wordt uitgevoerd.

Waarnemingen bewaren hun eigen eventgeschiedenis. De reactie vergelijkt de toestand vóór en na het betreffende rapport, ook na cachewissen of wanneer inmiddels nieuwere rapporten bestaan. Vorige meetwaarden worden niet als extra velden in het huidige rapport gekopieerd.

De rustperiode beperkt herhaald activeren. Een latere bronwijziging maakt een eerdere overgang niet uitsluitend vanwege een nieuwe revisie ongeldig: een huis hernoemen wist bijvoorbeeld geen vertrek, en een volgende meting boven de grens wist de eerdere grensoverschrijding niet. Alleen veranderingen aan thuismodus en gemelde sensortoestand starten de reactie; de gewenste instellingen die uit de scène volgen voeden geen lus terug.

Een gepauzeerde automatisering reageert niet. Hervatten activeert de bestaande situatie niet op zichzelf; een volgende passende verandering kan weer een scène activeren. De app houdt geen verwerkte bronrevisies of andere technische deduplicatieadministratie bij. Uitvoeringszekerheid bij herstel is een verantwoordelijkheid van SDK en Runtime. Volledige durable execution is voorzien, maar zit nog niet in de vastgelegde SDK-kandidaat; deze versie claimt daarom geen eenmaal-uitvoeringsgarantie bij opnieuw aangeboden events.

Een onuitvoerbare reactie pauzeert de automatisering met een reden. Tijdelijke technische storingen worden niet als domeinfout vermomd: de Fluxzero-consumer kan die opnieuw proberen.

De routineconsumer verwerkt planning en statuswijzigingen op één tracker en leest steeds de actuele bedoeling. Voor een afwezige routine doet hij niets: opruimen bij verwijdering hoort bij de SDK. Bij een inmiddels opnieuw aangemaakte routine reconcilieert ook een oude verwijdermelding uitsluitend de nieuwe actuele bedoeling.

## Wat de tests bewijzen

De tests verplaatsen de tijd; ze slapen niet. Ze controleren actieve schedules met exacte identiteit, deadline en generatie, plus de toestand direct vóór en op de uitvoering. `RoutineOwnershipTest` registreert geen routineconsumer en controleert zowel directe als cascaderende verwijdering, de bijbehorende `ScheduleAutoCancelled`-metric en bescherming van een opnieuw aangemaakte routine tegen oude opgeslagen uitvoeringen. Er zijn synchrone en asynchrone scenario's. Herladen na het legen van caches bewijst reconstructie uit opgeslagen Model-geschiedenis binnen de fixture; het is geen claim dat een externe productieopslag een procesrestart heeft doorstaan.
