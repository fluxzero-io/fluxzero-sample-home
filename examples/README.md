# Voorbeeldhuis

De JSON-commando's in deze map beschrijven een huis met een begane grond, woonkamer, tuin, leeslamp, verwarming en kamersensor. De scène *Een fijne avond* dimt de lamp en vraagt 21 °C. Een wekelijkse routine activeert die scène om 20:00 lokale tijd.

Aanmaak- en definitiecommands gebruiken `details`, met `@revision: 1`. Oude vlakke payloads met revisie 0 blijven leesbaar.

De bestanden staan in uitvoeringsvolgorde. De lokale ontwikkelconfiguratie laadt ze eenmaal per tijdelijke runtime. Ze bevatten geen echte apparaten of productiegegevens. Iedere nieuwe runtime begint opnieuw; de domeincommands weigeren bestaande identiteiten ongemerkt te overschrijven.

Voor uitgebreidere voorbeelden, waaronder scènes voor zones, klokovergangen, aanwezigheid en sensorreacties, zie de Java-gedragstests. De app zelf biedt in deze fase nog geen bedieningsscherm.

Een gewijzigd voorbeeldbestand kan de ontwikkelomgeving opnieuw laten aanbieden in dezelfde runtime. Een bestaande identiteit wordt dan terecht geweigerd. Gebruik een nieuwe tijdelijke runtime voor het opnieuw opbouwen van het voorbeeldhuis; wis hiervoor geen gegevens van een bestaande installatie.
