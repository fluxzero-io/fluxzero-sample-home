# Bestaande opslag behouden

De details-refactor en de wijziging van statusopslag bewaren de bestaande identiteit en geschiedenis van dit voorbeeldhuis. Oude events worden bij het lezen geïnterpreteerd volgens het huidige model. Ze worden niet vervangen of opnieuw als nieuwe handelingen aangeboden.

## Namen naar details

`Home`, `Space`, `Device`, `Resident`, `Zone`, `Scene`, `Routine` en `Automation` hebben nu revisie 1. Hun aanmaak- en definitiecommands hebben dezelfde nieuwe revisie. `DetailsUpcaster`, geregistreerd als Spring-component, leest de oorspronkelijke revisie 0:

- `name` wordt `details.name`.
- Bij `Space` en `AddSpace` wordt ook `kind` naar `details.kind` verplaatst.
- Identiteiten, relaties, instellingen, status, tijdstippen en tellers blijven behouden.

De conversie werkt zowel op historische command/events als op opgeslagen Modeldocumenten. Nieuwe berichten schrijven de nieuwe vorm. `RenameHome` en `RenameSpace` houden hun bestaande payload: alleen de naam verandert. De huidige JSON-voorbeelden vermelden expliciet `@revision: 1`.

De huidige applicatie zoekt op relaties en apparaatmogelijkheden. Die paden veranderen niet. Een nieuwe naamfilter op `details.name` vereist wel dat de betrokken oude zoekdocumenten opnieuw zijn geïndexeerd: deserialisatieconversie verandert de fysieke zoekindex niet. Hetzelfde geldt voor een nieuwe filter op `details.kind`. De app introduceert zulke filters niet in deze refactor.

## Apparaatstatus van rc.11

De oorspronkelijke Home-versie `86933ce2276964e47142f676b26efaecfe148e99` gebruikte `@Model(persistence = DOCUMENT)` voor `DeviceStatus`. De SDK gebruikte daarbij nog steeds de standaard `STORE_AND_PUBLISH`-strategie. De actuele waarde werd uit het document gelezen, maar de bijbehorende Model-events werden óók bewaard.

De huidige versie kan daardoor rechtstreeks dezelfde events lezen. Zij behoudt de laatste status, waarnemingstijden, bereikbaarheid, gerapporteerde instellingen en vorige metingen. `Graph.previous()` geeft de echte vorige waarneming. Het oude documentveld `previousReadings` was een afgeleide kopie en hoeft niet terug op het Model.

Er is voor deze oorspronkelijke configuratie geen schrijf- of backfillstap nodig. Het laden van de nieuwe versie voert de overgang uit, zonder extra waarneming, nieuwe revisie of activatie. Een eerste nieuwe rapportage vergelijkt met de werkelijk laatste oude waarneming. De oude directe statusdocumenten worden niet gewist; zij zijn na de overgang niet meer de bron voor `GetDeviceStatus`. Ook een achterblijvend direct document mag niet als actuele status worden gebruikt na nieuwe rapportages.

`StorageCompatibilityTest` begint met tien werkelijke opslagrequests uit de oude app op de gepubliceerde SDK `2.0.0-rc.11`, inclusief twee statusrapportages en hun directe documenten. De test controleert reconstructie zonder cache, de oude componentdocumenten, behoud van relaties, herhaalde reads zonder nieuwe events en een eenmalige activatie bij de eerste nieuwe grensoverschrijding. Het voorbeeld bevat ook een routine met een bestaande deadline.

Deze overgang geldt niet voor een installatie die zelf eventopslag uitschakelde of historie fysiek heeft gewist. Ontbrekende Model-historie wordt expliciet geweigerd; de app maakt daar niet stilzwijgend een lege of verzonnen beginstand van. Herstel in dat geval eerst de echte historie of ontwerp een afzonderlijke, expliciete beginstandmigratie voor die installatie. Meng geen oude en nieuwe schrijvers tijdens de overstap en bewaar bestaande opslag bij de deployment.
