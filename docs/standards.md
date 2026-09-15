# Matter and KNX as references

Home Assistant is this example's first concrete API integration. Matter and KNX help assess which home capabilities the model should describe. These three have different roles; the application does not yet implement its own Matter controller or KNX interface.

Matter describes interoperable device communication, including lighting, climate, locks, measurements and coverings. It uses local IP networks and can share devices across ecosystems. It is not a universal REST API for an existing home. Home Assistant has its own Matter integration and can expose the resulting entities through its API. See the [CSA FAQ](https://csa-iot.org/all-solutions/matter/matter-faq/) and [Home Assistant Matter integration](https://www.home-assistant.io/integrations/matter/).

KNX is relevant to complete installed homes, including lighting, blinds, climate and measurements. Home Assistant's KNX integration connects to the installation and maps it to entities. The Home model therefore does not need to know group addresses or datapoint types. See the [Home Assistant KNX integration](https://www.home-assistant.io/integrations/knx/).

The following assessment covers our domain and current adapter. It is not a declaration of conformity or a complete comparison of protocol versions.

| Home function | Current Home model | First HA adapter | Further work |
| --- | --- | --- | --- |
| Switching and dimming | `Power`, `LightLevel` | Yes, through `light` and `switch` | Transition duration could become a named domain value. |
| Light color | `LightColor` | Not yet | Color spaces, color temperature and supported ranges need explicit mappings. |
| Heating and cooling | `RoomTemperature` | Temperature readings only | A setpoint alone does not describe mode, heating/cooling or dual setpoints. |
| Coverings and windows | `Opening` | Not yet | Position is available; tilt angle, movement and stopping are not separate actions yet. |
| Access | `DoorLock`, `CONTACT_OPEN` | Contact readings only | Desired lock state and actual door position are already separate; obstruction and intermediate states need their own observations. |
| Sensors | Named measurements with fixed units | Explicit supported subset | Capturing every brief transition requires streaming instead of polling. |
| Media and ventilation | `Playback`, `Volume`, `FanSpeed` | Not yet | Match the actual device capabilities and reported state. |
| Water and charging | `Irrigation`, `Charging` | Not yet | A complete irrigation or energy plan needs more product concepts than on/off. |

The model provides a useful generic starting point, but “every home” does not mean every feature of every protocol already fits. Further capabilities should come from concrete use cases and documented device capabilities. Commands stay recognizable; missing features are not hidden in a generic map of technical properties.

Use a direct brand adapter when it adds a clearly wanted feature that this route cannot provide. The same lamp should retain one selected Home route, even when it is visible through Matter, Home Assistant and a brand platform at the same time.
