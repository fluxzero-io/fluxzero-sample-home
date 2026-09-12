# De integratiegrens

De eerste fase levert een uitvoerbare domeinkern. Het merk of protocol van een apparaat is geen reden om een ander huis-, ruimte- of scènemodel te maken.

Fase 2 voegt adapters toe voor drie bekende systemen. De keuze vraagt een vergelijking van actuele API-toegang, lokale versus cloudaansturing, beschikbare mogelijkheden en terugmeldingen. Er is in fase 1 geen rangorde of leverancierskeuze vastgezet.

De bestaande scheiding is:

1. De bewoner beschrijft een bedoeling met een command of scène.
2. De kern controleert de huisgrens en ondersteunde mogelijkheden en commit de gewenste instellingen.
3. Een latere adapter vertaalt gecommitteerde intenties naar het aangesloten systeem.
4. De adapter rapporteert de waargenomen toestand via `ReportDeviceStatus`, met de oorspronkelijke observatietijd.

Een adapter moet verbinding, externe apparaatidentiteit, secrets, afleverpogingen en fysieke bevestigingen zelfstandig beheren. Hij mag een gewenste instelling niet zelf als bewezen waarneming terugmelden. Herhaalde intenties en verbindingsherstel vragen reconciliatie met de actuele wens; ouder berichtverkeer mag geen nieuwere wens ongedaan maken.

Niet ieder apparaat heeft dezelfde grenzen. De huidige mogelijkheden zijn een breed begin voor licht, comfort, toegang, media, tuin en energie. Extra domeinfuncties krijgen benoemde, getypeerde instellingen met eigen regels. Er is bewust geen universeel zakje protocolvelden dat bewoners moeten begrijpen.

Een externe bedieningslaag zal de ingelogde gebruiker koppelen aan het huishouden en bevoegdheden per actie afdwingen. Sensorrapportages krijgen een eigen adapteridentiteit. `HouseholdRole` is nog geen implementatie van die authenticatiegrens. Er zijn in deze fase geen openbare bedieningsendpoints, accounts, API-sleutels of live apparaatkoppelingen geconfigureerd.
