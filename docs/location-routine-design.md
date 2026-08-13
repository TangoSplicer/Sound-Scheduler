# Location-Routine Design

## Scope

A location routine changes the phone to **Ring**, **Vibrate**, or **Silent** when the device **arrives at** or **leaves** a place the user creates while physically present. Places are local-only: the app does not provide search, maps, geocoding, sync, or location sharing.

## Data contract

The existing `routines` table gains nullable fields through a Room `2 → 3` migration. Existing time routines and legacy location records remain readable.

| Field | Type | Purpose |
| --- | --- | --- |
| `latitude` | nullable `Double` | Captured local latitude for a newly created location routine. |
| `longitude` | nullable `Double` | Captured local longitude for a newly created location routine. |
| `radiusMeters` | nullable `Int` | Selected circular boundary: 100, 150, 250, or 500 m. |
| `locationTransition` | nullable `String` | `enter` for arrival or `exit` for departure. |

A location routine is eligible for geofence registration only when it is enabled, has a local label, and has valid coordinates, radius, and transition data. This safely retains old records without registering malformed fences.

## Consent and recovery sequence

| Step | User action | App behavior |
| --- | --- | --- |
| 1 | Chooses “Location routine” | Explains the local-only feature and requests foreground precise location only when the user elects to capture their present place. |
| 2 | Captures current place | Reads one recent location locally, displays capture status, and lets the user choose a friendly label, radius, arrival/departure event, and target sound mode. |
| 3 | Saves the routine | Requests or directs the user to grant Android’s all-the-time location access, which is necessary for geofence delivery when the app is closed. |
| 4 | Returns from settings | Checks foreground, background, and Notification Policy access; registers eligible geofences only after all required access is present. |

The app never requests background location at launch, never receives continuous location updates, and never transmits coordinates.

## Geofence lifecycle

The `LocationRoutineManager` registers each valid active location routine using a single mutable broadcast `PendingIntent`. Arrival maps to `GEOFENCE_TRANSITION_ENTER`; departure maps to `GEOFENCE_TRANSITION_EXIT`. A 150 m default radius and five-minute responsiveness provide a power-conscious baseline. The manager removes a geofence on pause or delete, refreshes location fences after creation or resumed access, and rebuilds them after boot or package update.

The `LocationRoutineReceiver` verifies the transition, resolves the local routine by ID, applies its selected sound mode through the existing `SoundModeController`, and sends only a quiet optional confirmation. It ignores access-denied, paused, malformed, or mismatched triggers.

## User experience

The creation dialog gains a routine-type selector. **Time routine** preserves the current workflow. **Location routine** replaces time and recurrence fields with a transparent place-capture panel, an arrival/departure choice, and a radius choice. Routine cards state the target mode, place label, selected transition, and radius; the existing switch pauses or resumes either routine type.

## References

[1] [Android Developers — Create and monitor geofences](https://developer.android.com/develop/sensors-and-location/location/geofencing)

[2] [Android Developers — Request location permissions](https://developer.android.com/develop/sensors-and-location/location/permissions)

[3] [Android Developers — Access location in the background](https://developer.android.com/develop/sensors-and-location/location/background)
