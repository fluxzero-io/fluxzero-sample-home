# Scènes en tijd

## Een scène als één bedoeling

Een scène bevat geordende acties. Een actie richt zich op één apparaat, op een ruimte en haar onderliggende ruimtes, op een zone of op het hele huis.

Bij één specifiek apparaat moet dat apparaat de instelling ondersteunen. Bij een grotere doelgroep worden alleen apparaten met de gevraagde mogelijkheid geselecteerd. Als er geen geschikt apparaat is, wordt de hele scène geweigerd. Zones met overlappende ruimtes passen dezelfde uiteindelijke instelling maar één keer toe. Een latere actie mag een eerdere instelling verfijnen.

De selectie wordt opnieuw gemaakt bij iedere activatie. Daardoor hoort een lamp na verplaatsing bij haar nieuwe kamer. Alle controles en wijzigingen gebeuren op één vastgelegde Graph-toestand. Fluxzero voert de herkenbare deelhandelingen en de scène-activatie uit in één Model-transactie. Andere applicatiecomponenten krijgen geen half uitgevoerde scène te zien.

Dat is een garantie over de kern. Latere fysieke apparaten kunnen afzonderlijk bereikbaar zijn of falen; adapters zullen die voortgang afzonderlijk terugmelden.

## Een routine heeft een blijvende volgende uitvoering

`PlanRoutine` legt zowel het patroon als de concrete volgende uitvoering vast. Een post-commit consumer brengt de scheduler in overeenstemming met de actuele routine. Een opnieuw afgeleverd oud event kan daardoor geen gepauzeerde routine opnieuw plannen.

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

Deze automatische annulering is asynchroon en trekt reeds afgeleverde opdrachten niet terug. Daarom blijven de controles op actuele toestand, generatie en deadline nodig. Een nieuwe routine met hetzelfde ID krijgt een nieuwe levensduur: een eerder opgeslagen uitvoering kan niet opnieuw aan die nieuwe routine worden gekoppeld.

Als een scène door een gewijzigde huisinrichting niet meer uitgevoerd kan worden, wordt de routine gepauzeerd met een begrijpelijke reden in `problem`. Geen van de apparaatinstellingen wordt dan gedeeltelijk toegepast. Na herstel kan een herhalende routine hervat worden; een gemiste eenmalige routine kan opnieuw worden gepland.

## Reageren op veranderingen

Een automatisering kan reageren wanneer het huis een bepaalde modus krijgt, of wanneer een sensorwaarde een grens omhoog of omlaag passeert. Een eerste sensorwaarde bewijst nog geen grensovergang. Waarden die aan dezelfde kant van de grens blijven, activeren de scène niet opnieuw.

Waarnemingen bewaren hun eigen eventgeschiedenis. De reactie vergelijkt de toestand vóór en na het betreffende rapport, ook na cachewissen of wanneer inmiddels nieuwere rapporten bestaan. Vorige meetwaarden worden niet als extra velden in het huidige rapport gekopieerd.

De rustperiode beperkt herhaald activeren. De laatst verwerkte bronrevisie voorkomt dubbele verwerking. Een achterhaalde bronrevisie wordt genegeerd wanneer de bron intussen alweer is gewijzigd. Alleen veranderingen aan thuismodus en gemelde sensortoestand starten de reactie; de gewenste instellingen die uit de scène volgen voeden geen lus terug.

Een onuitvoerbare reactie pauzeert de automatisering met een reden. Tijdelijke technische storingen worden niet als domeinfout vermomd: de Fluxzero-consumer kan die opnieuw proberen.

De routineconsumer verwerkt planning en statuswijzigingen op één tracker en leest steeds de actuele bedoeling. Voor een afwezige routine doet hij niets: opruimen bij verwijdering hoort bij de SDK. Bij een inmiddels opnieuw aangemaakte routine reconcilieert ook een oude verwijdermelding uitsluitend de nieuwe actuele bedoeling.

## Wat de tests bewijzen

De tests verplaatsen de tijd; ze slapen niet. Ze controleren actieve schedules met exacte identiteit, deadline en generatie, plus de toestand direct vóór en op de uitvoering. `RoutineOwnershipTest` registreert geen routineconsumer en controleert zowel directe als cascaderende verwijdering, de bijbehorende `ScheduleAutoCancelled`-metric en bescherming van een opnieuw aangemaakte routine tegen oude opgeslagen uitvoeringen. Er zijn synchrone en asynchrone scenario's. Herladen na het legen van caches bewijst reconstructie uit opgeslagen Model-geschiedenis binnen de fixture; het is geen claim dat een externe productieopslag een procesrestart heeft doorstaan.
