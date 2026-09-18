# Setup — two manual steps

Both of these are tied to your own accounts/servers, so nobody can hand you
working files for either — this doc is the exact steps to do it yourself.

## 1. Point the app at your actual backend

`ApiClient.BASE_URL` used to be a hardcoded ngrok URL (which had already
expired — free ngrok tunnels rotate every time ngrok restarts). It's now read
from `local.properties` (gitignored, machine-specific — same idea as the
backend's `.env`), defaulting to `http://10.0.2.2:8080/api/` if you don't set
anything, which is the Android emulator's alias for "the host machine's
localhost:8080" — matches the backend's `docker-compose` port out of the box.

To point at something else (a real device on your LAN, a deployed server),
add to `local.properties`:
```
API_BASE_URL=http://192.168.1.50:8080/api/
```
Re-sync Gradle after changing it — `BuildConfig.API_BASE_URL` is baked in at
build time, not read at runtime.

## 2. Configure Firebase (needed for push notifications)

Nothing push-related can work — not receiving, not even initializing — until
this is done. It was never done before; there was no `google-services.json`
and the Google Services Gradle plugin was never applied.

1. Firebase Console → create/open your project → add an Android app with
   package name `com.example.supportassist`.
2. Download the `google-services.json` it gives you, put it at
   `app/google-services.json` (already gitignored — don't commit it).
3. Also do the backend half of this — see the backend's `RUNNING.md`,
   "Push Notifications (FCM)" section, for the service account key it needs.
4. Rebuild. The `google-services` plugin only applies itself once that file
   exists (see the comment in `app/build.gradle.kts`), so the app builds and
   runs fine before you do this too — you just won't get push until you do.

## What was actually broken before, if you're curious

See the chat history for the full list, but the short version: `SocketManager`
was a Socket.io client pointed at a dead tunnel with no server behind it
(deleted, replaced by `AlertPoller` — a long-poll client against the
backend's `/api/alerts/poll`), ticket image uploads were sending a local
file path as a JSON string instead of the actual file (fixed —
`CreateTicketActivity` now builds a real multipart request), the FCM token
was never sent to the backend even where the endpoint existed (fixed —
`FcmTokenSync`, called from `MainActivity` and `MyFirebaseMessagingService`),
and Room only ever got written to, never read from (fixed —
`TicketRepository` now falls back to the cache on a failed network call).
