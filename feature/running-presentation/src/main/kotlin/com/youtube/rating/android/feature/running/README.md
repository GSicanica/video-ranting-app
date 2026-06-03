# Running Feature

Standalone Android running feature with:

- manual run entry
- GPS route tracking (foreground)
- route preview on Google Maps
- local persistence of runs and route points
- foreground service notification while GPS session is active

## Main files

- `domain/RunningEntry.kt`
- `domain/RoutePoint.kt`
- `data/RunningRepository.kt`
- `location/AndroidRouteTracker.kt`
- `service/RunningTrackingService.kt`
- `service/RunningTrackingServiceController.kt`
- `viewmodel/RunningViewModel.kt`
- `ui/RunningScreen.kt`

## Notes

- Maps key is read from `GOOGLE_MAPS_API_KEY` in `local.properties` via manifest placeholder.
- GPS tracking now starts a location foreground service for better background reliability.

