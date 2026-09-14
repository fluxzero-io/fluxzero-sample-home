# Matter en KNX als referentie

Home Assistant is de eerste concrete API-koppeling van dit voorbeeld. Matter en KNX helpen beoordelen welke huisfuncties het model moet kunnen beschrijven. Deze drie hebben verschillende rollen; de app implementeert nog geen eigen Matter-controller of KNX-interface.

Matter beschrijft interoperabele apparaatcommunicatie, bijvoorbeeld voor verlichting, klimaat, sloten, metingen en zonwering. Het gebruikt lokale IP-netwerken en kan apparaten met meerdere ecosystemen delen. Het is geen universele REST-API naar een bestaand huis. Home Assistant heeft zijn eigen Matter-integratie en kan de daaruit ontstane entities via zijn API tonen. Zie de [CSA-uitleg](https://csa-iot.org/all-solutions/matter/matter-faq/) en [Home Assistant Matter-integratie](https://www.home-assistant.io/integrations/matter/).

KNX is daarnaast relevant voor complete geïnstalleerde woningen: onder meer verlichting, jaloezieën, klimaat en meetwaarden. Home Assistant verzorgt via zijn KNX-integratie de verbinding met de installatie en de vertaling naar entities. Het Home-model hoeft daardoor geen groepsadressen of datapointtypes te kennen. Zie de [Home Assistant KNX-integratie](https://www.home-assistant.io/integrations/knx/).

De onderstaande beoordeling betreft ons domein en de huidige adapter. Het is geen conformiteitsverklaring of volledige vergelijking van protocolversies.

| Huisfunctie | Huidig Home-model | Eerste HA-adapter | Wat een verdere uitwerking vraagt |
| --- | --- | --- | --- |
| Schakelen en dimmen | `Power`, `LightLevel` | Ja, via `light` en `switch` | Overgangsduur kan later een benoemde domeinwaarde worden. |
| Lichtkleur | `LightColor` | Nog niet | Kleurruimten, kleurtemperatuur en ondersteunde bereiken moeten expliciet worden vertaald. |
| Verwarming en koeling | `RoomTemperature` | Alleen temperatuurmetingen | Een setpoint alleen beschrijft geen modus, koelen/verwarmen of dubbel setpoint. |
| Zonwering en ramen | `Opening` | Nog niet | Stand is aanwezig; lamelhoek, bewegen en stoppen zijn nog geen afzonderlijke handelingen. |
| Toegang | `DoorLock`, `CONTACT_OPEN` | Alleen contactmeting | Gewenst slot versus feitelijke deurstand zijn al gescheiden; blokkering en tussenstanden vragen eigen observaties. |
| Sensoren | Benoemde metingen met vaste eenheden | Expliciete ondersteunde subset | Voor alle korte overgangen is streaming nodig in plaats van polling. |
| Media en ventilatie | `Playback`, `Volume`, `FanSpeed` | Nog niet | Afstemming op de feitelijke apparaatmogelijkheden en terugmeldingen. |
| Water en laden | `Irrigation`, `Charging` | Nog niet | Een uitgebreid irrigatie- of energieplan vraagt meer producttaal dan aan/uit. |

Het model heeft daarmee een bruikbaar generiek begin, maar “elk huis” is geen claim dat alle eigenschappen van alle protocollen al passen. De volgende functies voegen we toe vanuit concrete gebruiksscenario's en gedocumenteerde apparaatmogelijkheden. Commands blijven herkenbaar; ontbrekende functies worden niet verstopt in een algemene map met technische properties.

Gebruik een directe merkadapter wanneer die een duidelijk gewenste functie toevoegt die deze route nog niet kan bieden. Dezelfde lamp mag daarbij één gekozen Home-route houden, ook als hij tegelijkertijd via Matter, Home Assistant en een merkplatform zichtbaar is.
