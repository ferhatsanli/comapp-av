# Firebase PING command foundation

## Target

- Firebase project: `comapp-av`
- Realtime Database: `https://comapp-av-default-rtdb.europe-west1.firebasedatabase.app`
- Android app: `com.ferhat.comappav`
- Firebase App ID: `1:719144763397:android:05f1f90b7dfd564c235c6c`
- Android test UID: `57HVnDSvZKNbJBabrDF4c2gnv462`
- Raspberry Pi agent UID: `N8V4nrLrELa83EXRLWJsiis85Bq1`

## Authentication state verified

Email/Password sign-in is enabled and requires a password. The two supplied UIDs exist and use the `password` provider. No anonymous provider or external OAuth/OIDC provider configuration was found. No Auth users, passwords, or provider settings were changed.

## Schema

Commands are stored at `/commands/{commandId}`. Android obtains a unique `commandId` using a Realtime Database push key, writes the four fields below, then observes that exact child path. It must not listen at `/commands`, because that would require permission to read every sender's commands.

| Field | Type | Allowed value |
| --- | --- | --- |
| `action` | string | `PING` |
| `senderUid` | string | `57HVnDSvZKNbJBabrDF4c2gnv462` |
| `targetUid` | string | `N8V4nrLrELa83EXRLWJsiis85Bq1` |
| `status` | string | `PENDING`, `PROCESSING`, `SUCCESS`, or `FAILED` |
| `processingAt` | number, while processing only | Server timestamp in milliseconds; refreshed when a stale claim is recovered |
| `result` | string, final state only | `PONG` when status is `SUCCESS` |
| `errorCode` | string, final state only | `AGENT_ERROR` when status is `FAILED` |

`PENDING` commands contain exactly the four core fields. `processingAt` exists only while processing, must be numeric, and cannot be more than 30 seconds old when claimed or recovered. No arbitrary message, shell command, error text, action arguments, or unknown fields are accepted.

Allowed transitions:

```text
Android creates:       (absent) -> PENDING
Pi agent claims:       PENDING -> PROCESSING + processingAt
Pi agent recovers:     stale PROCESSING -> PROCESSING + refreshed processingAt
Pi agent completes:    PROCESSING -> SUCCESS + result=PONG
Pi agent fails:        PROCESSING -> FAILED + errorCode=AGENT_ERROR
```

A processing claim older than two minutes can be recovered. The Pi agent uses an ETag conditional update when claiming or recovering, so concurrent agent instances cannot both acquire the same claim. PING has no external side effect, so repeating its connectivity check after a crash is safe.

The Android UID is fixed to the verified test identity for this milestone. The Pi UID is fixed to its verified identity. The Pi can query `/commands` only when ordering by `targetUid` and filtering with `equalTo` its own UID. The query is indexed by `targetUid`. Android reads only `/commands/{commandId}` after creating it; the per-command rule checks that its stored `senderUid` matches the caller. Realtime Database read/write grants cascade, so the constrained Pi query rule lives at `/commands`, while Android ownership is checked at the individual command.

Rules reject unknown actions, sender impersonation, Android status changes, Pi changes to `action`, `senderUid`, or `targetUid`, skipped status transitions, unknown fields, deletes, and all unauthenticated access.

## Files and deployment

- `database.rules.json` is the reviewed Realtime Database Rules source.
- `firebase.json` pins the named Realtime Database instance and the emulator port.
- `.firebaserc` selects `comapp-av` as this checkout's default Firebase project.
- `app/google-services.json` is the Firebase-provided Android config. It is intentionally not copied into documentation.

Deploy only the configured database rules with:

```sh
firebase deploy --only database --project comapp-av
```

The configured `comapp-av-default-rtdb` instance is the verified default database at `https://comapp-av-default-rtdb.europe-west1.firebasedatabase.app`.

## Run Rules tests

Install the Node test dependencies once with `npm install`. The tests use only the Realtime Database Emulator and mock UID claims. They never connect to production:

```sh
npm run test:firebase-rules
```

Expected test output is one passing summary covering denied unauthenticated reads/writes, Android ownership and PENDING-only creation, Pi filtering and transitions, immutable fields, unexpected fields, and deletion.
