# Het huis als domein

Een huis vormt de samenhang, niet één groot opslagobject. Een lamp kan worden vervangen zonder de geschiedenis van een kamer te herschrijven. Een routine kan verdwijnen terwijl haar scène blijft bestaan.

| Begrip | Betekenis en levenscyclus |
| --- | --- |
| `Home` | Naam, lokale tijdzone en gebruik: thuis, afwezig, slapen of vakantie. |
| `Space` | Een gebouw, verdieping, kamer, buitenruimte of andere plek; heeft één plaats in een recursieve boom. |
| `Zone` | Een benoemde verzameling ruimtes. Overlap is toegestaan; een zone bezit haar ruimtes niet. |
| `Resident` | Een bewoner met eigen naam, huishoudrol en aanwezigheid. |
| `Device` | Een apparaat op een plek, met mogelijkheden, ondersteunde metingen en gewenste instellingen. |
| `DeviceStatus` | Apparaatwaarnemingen met tijd, bereikbaarheid, instellingen en metingen. De actuele waarde is het laatste volledige rapport; de eigen historie levert vorige metingen voor grensdetectie. |
| `Scene` | Een benoemde verzameling bedoelingen, inclusief het aantal activaties en de laatste activatie. |
| `Routine` | Een scène met een tijdpatroon, volgende uitvoering, pauzestand en uitvoeringsgeschiedenis. |
| `Automation` | Een scène gekoppeld aan een betekenisvolle verandering, met pauzestand, rustperiode en uitvoeringsgeschiedenis. |

Alle bovenstaande begrippen zijn zelfstandige Models. De relaties die bezit uitdrukken zijn `@Parent`-relaties. Scene-acties en instellingen zijn waardes: ze hebben geen eigen identiteit of levenscyclus. Daarom worden ze niet kunstmatig tot Models of Members gemaakt.

## Beschrijvende gegevens

Ieder benoembaar begrip heeft zijn eigen immutable details-value-object: `HomeDetails`, `SpaceDetails`, `DeviceDetails`, `ResidentDetails`, `ZoneDetails`, `SceneDetails`, `RoutineDetails` en `AutomationDetails`. De naam hoort daarin, met dezelfde grens van 1–120 tekens. `SpaceDetails` bevat ook de soort ruimte: samen beschrijven die wat de plek is.

Identiteit en relaties staan op het Model. Het apparaatlabel blijft een alternatieve identiteit; mogelijkheden, gewenste instellingen, huishoudrol en tijdzone behouden hun eigen betekenis. Actuele status, volgende uitvoering, generaties en tellers behoren evenmin tot de beschrijving. Nieuwe beschrijvende velden kunnen later binnen het passende details-object worden toegevoegd.

`CreateHome` en `AddSpace` ontvangen de volledige beschrijving. `RenameSpace` ontvangt alleen de nieuwe naam en behoudt onder meer de soort ruimte, ouder en het primaire licht. De invoer valideert ook geneste details en weigert ontbrekende waarden.

## Indeling en grenzen

Een ruimte heeft een `homeId` en eventueel een `enclosingSpaceId`. De daadwerkelijke ouder is óf het huis óf die andere ruimte. Daardoor verschijnt iedere ruimte op precies één plek in de huis-Graph. De SDK weigert concrete cycli; de app weigert verplaatsen tussen verschillende huizen.

Apparaten behoren aan hun ruimte. Een verplaatsing ruimt ook de voorkeur voor het primaire licht in de oude kamer op. `RemoveDevice` kan dezelfde voorkeur via de bestaande ouderrelatie opruimen, zonder een ruimte-ID in het command. Een expliciete apparaatverwijzing uit een scène of sensortrigger voorkomt verwijdering tot die verwijzing is opgeruimd.

Een ruimte kan pas weg als zij leeg is en niet meer expliciet in een zone of scène voorkomt. Het logisch verwijderen van een heel huis verwijdert zijn eigendom en annuleert resterende routines. Logische verwijdering is geen fysieke uitwissing van geschiedenis.

## Een apparaat heeft mogelijkheden

Een slimme speaker kan geluid afspelen en volume instellen. Een klimaatunit kan temperatuur en ventilatiesnelheid aanbieden. Een gewone sensor kan uitsluitend meten. Het model schrijft geen vaste apparaatcategorie voor.

Herkenbare commands zijn onder meer `TurnOn`, `TurnOff`, `DimLight`, `SetLightColor`, `SetRoomTemperature`, `SetOpening`, `LockDoor`, `UnlockDoor`, `PlayMedia`, `SetVolume`, `SetFanSpeed`, `StartWatering`, `StopWatering`, `EnableCharging` en `PauseCharging`.

Percentages lopen van 0 tot 100. De kleurtoon loopt van 0 tot 359. De huidige comfortinstelling voor een ruimte loopt van 5 tot 35 °C. Dit is een expliciete grens van de comfortfunctie; bijzondere installaties zoals een sauna krijgen een eigen herkenbare instelling met een passend bereik, in plaats van deze grens stilzwijgend te verruimen.

Metingen hebben een vaste eenheid: bijvoorbeeld °C, %, lx, W, kWh of ppm. Aan/uitwaarnemingen zoals beweging en waterlekkage gebruiken 0 en 1. Een oudere of gelijke waarneming overschrijft nooit een nieuwere. Een rapport is een volledige actuele waarneming; niet meegeleverde metingen zijn onbekend in dat rapport.

## Wens, waarneming en toegang

`Device.desiredSettings` beschrijft wat gevraagd is. `DeviceStatus.reportedSettings` beschrijft wat een adapter heeft waargenomen. `Availability` zegt iets over bereikbaarheid. De kern voegt deze betekenissen niet samen.

Een bewonersrol zoals eigenaar of gast is op dit moment een huishoudbegrip. De identiteit en bevoegdheid van een API-gebruiker moeten bij het aanbieden van externe toegang aan deze domeingrenzen worden gekoppeld. Er bestaat nog geen publiek endpoint waarmee onbevoegden huizen kunnen bedienen.
