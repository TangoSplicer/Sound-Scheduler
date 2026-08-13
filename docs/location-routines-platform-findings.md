# Location-Routine Platform Findings

## Product decision

Location routines will remain **entirely on-device**. A user creates a named place while physically present, captures the current coordinates locally, selects a 100–500 m radius, chooses an arrival or departure trigger, and selects Ring, Vibrate, or Silent. The app does not geocode, transmit, synchronize, or otherwise share location data.

## Android requirements

| Concern | Implementation consequence |
| --- | --- |
| Geofencing permission | The app must declare precise foreground location and, for off-screen geofence delivery on Android 10+, background location. |
| User consent | Foreground location is requested only after the user starts to create a location routine. Background access is explained separately and routed to Android’s app settings where required. |
| Accuracy | Geofences default to 150 m and allow 100, 250, or 500 m options; Android recommends a minimum radius of roughly 100–150 m for practical Wi-Fi and network-location accuracy. |
| Trigger delivery | A manifest receiver handles arrival and departure broadcasts. It does not launch a visible screen and applies only the routine’s configured sound mode. |
| Battery and latency | Geofences are event-driven, not continuously polled. Android may delay background events by a few minutes, so the feature must not be presented as a precise real-time switch. |
| Lifecycle | Active geofences are re-registered after device restart and package update. Paused and deleted routines are removed from monitoring. |
| Privacy | Coordinates, radius, and trigger choice stay solely in the local Room database and are used only to maintain the user-created geofence. |

## Implementation guardrails

The app will cap its own location routines below Android’s 100-geofence-per-app limit, reject missing captured coordinates, and retain legacy location records safely without attempting to register a malformed fence. Sound-mode changes still require user-granted Notification Policy access; a location transition never bypasses that Android control.

## References

[1] [Android Developers — Create and monitor geofences](https://developer.android.com/develop/sensors-and-location/location/geofencing)

[2] [Android Developers — Request location permissions](https://developer.android.com/develop/sensors-and-location/location/permissions)

[3] [Android Developers — Access location in the background](https://developer.android.com/develop/sensors-and-location/location/background)
