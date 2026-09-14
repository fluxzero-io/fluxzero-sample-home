# Home Assistant als eerste integratie

Dit voorbeeld volgt de openbare [Home Assistant REST API](https://developers.home-assistant.io/docs/api/rest/). Het ontdekt entities, koppelt ze bewust aan bestaande apparaten, vertaalt gecommitteerde wensen naar serviceacties en haalt gemelde toestanden periodiek op. De app start en haar tests draaien zonder Home Assistant of toegangstoken.

Lees voor de flow eerst `HomeAssistantTest`: de gewone huiscommands blijven het uitgangspunt. `HomeAssistantApiTest` gebruikt een lokale HTTP-testserver om de requestmethode, URL, bearer-header, JSON, foutafhandeling en het uitblijven van redirects te controleren. Dit zijn API-contracttests, geen kwalificatie met fysieke apparatuur.

## Het kleinste complete voorbeeld

De bestaande voorbeelddata bevat `example-home` en `example-light`. Om die leeslamp met een eigen installatie te verbinden, configureert de operator twee waarden op de machine waar de **Home-app** draait:

| Property | Omgevingsvariabele | Voorbeeld |
| --- | --- | --- |
| `home-assistant.demo.url` | `HOME_ASSISTANT_DEMO_URL` | `http://homeassistant.local:8123` |
| `home-assistant.demo.token` | `HOME_ASSISTANT_DEMO_TOKEN` | Een eigen long-lived access token |

Maak het token in het gebruikersprofiel van Home Assistant zoals beschreven in de REST-handleiding. Lever het via de deploymentconfiguratie of de bestaande versleutelde Fluxzero-configuratie aan; zet het niet in Git of command-JSON. Gebruik HTTPS wanneer de verbinding buiten een vertrouwd lokaal netwerk loopt. De gewone `ApplicationProperties`-resolutie verzorgt configuratie en fixture-overrides.

Het huis kiest alleen de naam van een vooraf geconfigureerde groep. Een command kan geen token of willekeurige doel-URL aanleveren. Verkeer gaat rechtstreeks vanuit deze adapter naar Home Assistant; de bearer-header wordt niet als Fluxzero-webbericht opgeslagen. De HTTP-client volgt geen redirects, gebruikt een timeout van vijf seconden en neemt remote foutinhoud niet op in foutmeldingen.

Vanuit een vertrouwde applicatiecomponent:

```java
var connection = new HomeAssistantId("demo");
var home = new HomeId("example-home");
var light = new DeviceId("example-light");

Fluxzero.sendCommandAndWait(new ConnectHomeAssistant(connection, home,
        new HomeAssistantDetails("Mijn Home Assistant", "demo"), Duration.ofSeconds(10)));

var found = Fluxzero.queryAndWait(new DiscoverHomeAssistantDevices(connection));
// Kies uit found een entity met POWER en LIGHT_LEVEL; gebruik haar echte entityId.
Fluxzero.sendCommandAndWait(new LinkHomeAssistantDevice(light, connection, Set.of("light.reading")));

Fluxzero.sendCommandAndWait(new DimLight(light, new LightLevel(25)));
var observed = Fluxzero.queryAndWait(new GetDeviceStatus(light));
```

`observed` kan direct na het command nog de vorige toestand bevatten. Eerst commit Home de wens, daarna voert de adapter de serviceactie uit. Een volgende refresh rapporteert de toestand die Home Assistant kent. De REST-route `POST /api/states/...` wordt nooit gebruikt voor fysieke aansturing; de adapter gebruikt `POST /api/services/light/turn_on` met `entity_id` en `brightness_pct`. Een HTTP-succes wordt niet zelf een statusrapport. Zie de [REST API](https://developers.home-assistant.io/docs/api/rest/) en [lichtacties](https://www.home-assistant.io/integrations/light/).

Een ondersteunde entity mag méér kunnen dan het gekozen Home-apparaat. De koppeling moet wel alle gedeclareerde apparaatmogelijkheden en metingen afdekken. Voorbeeld: een RGB-lamp kan voorlopig worden gekoppeld als apparaat met uitsluitend `POWER` en `LIGHT_LEVEL`; een apparaat dat ook `LIGHT_COLOR` declareert wordt geweigerd totdat die vertaling is toegevoegd.

Dezelfde entities worden binnen één verbinding niet aan verschillende apparaten gekoppeld. Een apparaat heeft één Home Assistant-koppeling. Een ander systeem dat dezelfde fysieke lamp toont, is geen reden om nogmaals een Device te maken. Een latere tweede adapter moet die ene gekozen route behouden.

Een Home Assistant-entity is niet altijd een heel fysiek apparaat. De kamersensor uit het voorbeeld kan bijvoorbeeld twee entities gebruiken:

```java
Fluxzero.sendCommandAndWait(new LinkHomeAssistantDevice(
        new DeviceId("example-sensor"), connection,
        Set.of("sensor.room_temperature", "binary_sensor.room_motion")));
```

Eén snapshot bevat beide metingen. Twee bronnen voor dezelfde meting of twee controllers voor dezelfde mogelijkheid worden als ambigu afgewezen. Ontdekken maakt geen ruimtes of apparaten aan en verandert hun naam niet. Apparaat en verbinding moeten bij hetzelfde huis horen.

## Ondersteunde vertalingen

| Home Assistant | Home | Gedrag |
| --- | --- | --- |
| `light` | `Power`, eventueel `LightLevel` | Aan/uit en dimpercentage; dimmen alleen met een passend `supported_color_modes`. |
| `switch` | `Power` | Expliciet `turn_on` / `turn_off`; geen toggle. |
| `sensor`, klasse temperature | `TEMPERATURE` | °C of °F naar °C. |
| humidity / illuminance / battery / carbon_dioxide | Gelijknamige meting | Alleen %, lx, % en ppm. |
| power / energy | `POWER` / `ENERGY` | W/kW naar W; Wh/kWh naar kWh. |
| `binary_sensor`, motion / occupancy / presence | `MOTION` | `on` = 1; `off` = 0. |
| door / window / opening | `CONTACT_OPEN` | `on` = open. |
| smoke / moisture | `SMOKE` / `WATER_LEAK` | `on` = gedetecteerd. |

De bron voor de classificatie en attributen is de documentatie voor [licht](https://developers.home-assistant.io/docs/core/entity/light/), [sensoren](https://developers.home-assistant.io/docs/core/entity/sensor/) en [binaire sensoren](https://developers.home-assistant.io/docs/core/entity/binary-sensor/). Onbekende domeinen, eenheden en niet-ondersteunde mogelijkheden worden niet als werkende ondersteuning geadverteerd.

Bij gecombineerde lichtwensen gaat expliciet uit vóór dimmen; nul helderheid betekent eveneens uit. De kern bewaart de gekozen dimensies onafhankelijk. Een API-refresh blijft uitsluitend waarnemen: handmatige bediening wordt niet iedere tien seconden teruggedraaid naar een oude, al afgeleverde wens.

Kleur, klimaatbediening, zonwering, sloten, media, ventilatie, irrigatie en laden zijn nog niet naar Home Assistant vertaald. Zij blijven wel in het generieke core model beschikbaar. Uitbreiden betekent een concrete mapping en protocolvoorbeeld toevoegen, inclusief eenheden, geldige bereiken en betekenis van de terugmelding.

## Scheduling, storingen en verwijderen

Een `RefreshHomeAssistant`-schedule hoort via `@Parent` bij de verbinding. De refreshinterval is instelbaar tussen vijf seconden en één uur. De volgende refresh wordt ook bij een verbindingsfout gepland; `GetHomeAssistant` toont die huidige fout. Een onbereikbare gateway zegt niets met zekerheid over een lamp: de adapter vervangt haar laatste waarneming dan niet door verzonnen offline metingen.

Een ontbrekende of door Home Assistant als `unavailable` gemelde entity geeft wel een offline apparaatrapport. `unknown`, ontbrekende benodigde waarden en ongeldige meetwaarden geven onbekende toestand zonder verzonnen nulwaarden. De adapter vergelijkt complete rapporten en publiceert alleen gewijzigde inhoud. `observedAt` is het moment waarop de adapter de gecombineerde API-snapshot bemonstert, niet een nieuwe fysieke meettijd voor iedere afzonderlijke sensor. De verschillende HA-velden `last_updated` worden niet tot één fictieve oorspronkelijke sensortijd samengevoegd.

Na een mislukte serviceactie bewaart de koppeling een actuele afleverfout en plant de adapter `DeliverHomeAssistantSettings`. Die schedule bevat apparaat en verbinding, zonder gekopieerde instellingen. De herpoging leest de nieuwste apparaatwens. Na succes verdwijnt de afleverfout en stopt de herpoging; de fysieke terugmelding komt nog steeds uit een aparte refresh.

Geplande pogingen en gewone apparaatwijzigingen komen voor fysieke uitvoering samen op één eventconsumer. Daardoor is er één uitvoeringsvolgorde, zonder een eigen threadpool of verbindingsmanager. Dat begrenst de doorvoer van dit voorbeeld: één traag HTTP-request kan andere Home Assistant-effecten enkele seconden ophouden. Een grotere toepassing kan dit gericht per installatie opdelen.

```java
// Alleen deze route verwijderen; het Device blijft bestaan.
Fluxzero.sendCommandAndWait(new UnlinkHomeAssistantDevice(light));

// Verbinding en haar routes verwijderen; de apparaten en huisindeling blijven bestaan.
Fluxzero.sendCommandAndWait(new DisconnectHomeAssistant(connection));
```

Verwijderen annuleert opgeslagen werk via parent-ownership. Een observatie controleert haar nog gekozen route voordat zij in het apparaatmodel terechtkomt. Een reeds verstuurd HTTP-request kan niet worden teruggeroepen. De adapter claimt geen transactie over fysieke apparaten: een scène commit al haar wensen atomair, maar hun fysieke aflevering kan gedeeltelijk slagen. Er is geen extra applicatieadministratie voor generieke dubbele eventaflevering; de huidige SDK-pin belooft nog geen volledige durable execution.

## Waarom deze eerste variant pollt

De [WebSocket API](https://developers.home-assistant.io/docs/api/websocket/) ondersteunt `state_changed`. Deze eerste variant gebruikt volledige REST-snapshots en de Fluxzero-scheduler. Dat houdt de eerste integratie klein en laat planning, onafhankelijke Modellevenscycli en gecommitteerde effecten zien zonder een eigen socketlifecycle.

Een korte beweging of open/dicht-overgang tussen twee polls kan daardoor ontbreken. Dit voorbeeld is geschikt om de architectuur, bedieningen en meetgrensreacties te leren; het implementeert geen betrouwbare alarmcentrale. Voor alle overgangen wordt de inkomende route later vervangen door een WebSocket-subscriptie met snapshot bij herstel. Commands, scènes en `ReportDeviceStatus` hoeven daarvoor niet te veranderen.

Er zijn geen openbare bedieningseindpunten. De commands en queries zijn bedoeld voor vertrouwde componenten. Een gebruikersinterface of publieke API vereist eerst huishoudgebonden authenticatie en autorisatie.
