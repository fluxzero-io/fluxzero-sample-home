# De integratiegrens

De eerste fase levert een uitvoerbare domeinkern. Het merk of protocol van een apparaat is geen reden om een ander huis-, ruimte- of scènemodel te maken.

De eerste fase-2-adapter gebruikt de [Home Assistant REST API](home-assistant.md). [Matter en KNX](standaarden.md) zijn referenties voor het generieke model. De adapter ondersteunt een expliciete eerste subset; een breed core model betekent niet dat iedere API-functie al vertaald is.

De bestaande scheiding is:

1. De bewoner beschrijft een bedoeling met een command of scène.
2. De kern controleert de huisgrens en ondersteunde mogelijkheden en commit de gewenste instellingen.
3. Een adapter vertaalt gecommitteerde intenties naar het aangesloten systeem.
4. De adapter rapporteert de waargenomen toestand via `ReportDeviceStatus`, met de tijd waarop hij die toestand heeft waargenomen. De REST-adapter assembleert meerdere entities tot één snapshot; hij gebruikt daarvoor het bemonsteringsmoment en verwart dat niet met een nieuwe fysieke sensormeting.

Een adapter moet verbinding, externe apparaatidentiteit, secrets, afleverpogingen en fysieke bevestigingen zelfstandig beheren. Hij mag een gewenste instelling niet zelf als bewezen waarneming terugmelden. Herhaalde intenties en verbindingsherstel vragen reconciliatie met de actuele wens; ouder berichtverkeer mag geen nieuwere wens ongedaan maken.

Niet ieder apparaat heeft dezelfde grenzen. De huidige mogelijkheden zijn een breed begin voor licht, comfort, toegang, media, tuin en energie. Extra domeinfuncties krijgen benoemde, getypeerde instellingen met eigen regels. Er is bewust geen universeel zakje protocolvelden dat bewoners moeten begrijpen.

Een externe bedieningslaag zal de ingelogde gebruiker koppelen aan het huishouden en bevoegdheden per actie afdwingen. Sensorrapportages krijgen een eigen adapteridentiteit. `HouseholdRole` is nog geen implementatie van die authenticatiegrens. Er zijn geen openbare bedieningsendpoints of standaard live apparaatkoppelingen. Wie Home Assistant aansluit, levert zelf de configuratiegroep en kiest expliciet de apparaatkoppelingen.
