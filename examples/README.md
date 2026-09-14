# Voorbeeldhuis

De JSON-commando's in deze map beschrijven een huis met een begane grond, woonkamer, tuin, leeslamp, verwarming en kamersensor. De scène *Een fijne avond* dimt de lamp en vraagt 21 °C. Een wekelijkse routine activeert die scène om 20:00 lokale tijd.

Aanmaak- en definitiecommands gebruiken `details`. Scèneacties hebben een eigen `kind`, zoals `dimLights` of `setHeating`, met een concrete waarde (`brightness` of `temperature`) en een doel zoals `{"kind":"space","spaceId":"example-living"}`. De voorbeelden gebruiken het huidige schema zonder expliciete schemarevisies of conversie van oude payloads.

`AddSpace` kiest één ouder met `"parentId": ["home", "example-home"]` of `"parentId": ["space", "example-floor"]`. Het command vraagt geen afzonderlijk huis naast een bovenliggende ruimte. De soort in deze verwijzing onderscheidt ook een huis en ruimte met dezelfde lokale ID.

De bestanden staan in uitvoeringsvolgorde. De lokale ontwikkelconfiguratie laadt ze eenmaal per tijdelijke runtime. Ze bevatten geen echte apparaten of productiegegevens. Iedere nieuwe runtime begint opnieuw; de domeincommands weigeren bestaande identiteiten ongemerkt te overschrijven.

Voor uitgebreidere voorbeelden, waaronder scènes voor zones, klokovergangen, aanwezigheid en sensorreacties, zie de Java-gedragstests. De app zelf biedt in deze fase nog geen bedieningsscherm.

Een gewijzigd voorbeeldbestand kan de ontwikkelomgeving opnieuw laten aanbieden in dezelfde runtime. Een bestaande identiteit wordt dan terecht geweigerd. Gebruik een nieuwe tijdelijke runtime voor het opnieuw opbouwen van het voorbeeldhuis; wis hiervoor geen gegevens van een bestaande installatie.

De optionele [Home Assistant-handleiding](../docs/home-assistant.md) bouwt verder op dit huis. De gewone voorbeelddata maakt geen externe verbinding en bevat geen credentials. `HomeAssistantTest` laat ook een waargenomen temperatuur via een gewone Home-automatisering een lamp bedienen.
