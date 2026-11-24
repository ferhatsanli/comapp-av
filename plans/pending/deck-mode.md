# Deck Mode

A full-screen control deck for when the phone sits next to the computer: big Favorite Apps
tiles and media controls, one tap each, with room for more panels later.
Ticket: none yet (requested in chat, 2026-11-24).

Status: **questions answered, waiting for approval.**

## What exists today (research)

- `MainActivity.kt` is one composable; every card is a local lambda sharing the screen's state
  (`favoriteApps`, `serverApps`, `mediaVolume`, `sendCommand`, `canSend`, the status light).
  Settings already shows how an overlay over that screen works (`SettingsSheet`, Back closes it).
- Favorites: `FavoriteApps` + prefs `favorites.<deviceId>`, shown in the Apps card as
  `favoriteRows × 4` tiles (`AppTile`, long press toggles favorite).
- Media: `PingProtocol.mediaCommand(kind, volume)`; the agent reports `media/latest`
  (volume, muted). No track title or artist is reported anywhere yet.
- The app sends **one command at a time**: `canSend` is false while the previous one is
  PENDING/PROCESSING. The agent itself walks every queued command, so several in a row are
  safe on its side (`process_pending` iterates all of them).
- Commands category = SPEAK, MEDIA, APPS, SYSTEM_UPDATE; cards are reorderable, hidden by
  capability (`CardId.capability`).
- Strings come from `tools/strings/*.py` (EN/TR/NL); Robolectric UI tests exist for Settings,
  AppTile, the status light.

## Phases

### Phase 1: grid engine (pure Kotlin, highest risk first)
Verified when: unit tests prove packing, resizing limits and reordering for both orientations.
Files to touch: `DeckGrid.kt` (new), `DeckGridTest.kt` (new).

#### Step 1.1: Grid model and packing (Size: S)
- Acceptance criteria: the screen is 8×4 units in landscape and 4×8 in portrait; panels are
  placed in the saved order, each at the first free spot that fits (left to right, top to
  bottom); a panel wider than the grid is narrowed to it; panels that don't fit go below and
  the deck scrolls.
- Verification command: `./gradlew testDebugUnitTest --tests '*DeckGridTest*'` (CI)
- Files likely touched: `DeckGrid.kt`
- Test seams: `DeckGrid.pack(panels, columns)` → positions
- Blocked by: none

#### Step 1.2: Resize and reorder rules, saved (Size: S)
- Acceptance criteria: each panel kind has a minimum and maximum size (Favorites 1×1 to 8×4,
  Media 2×1 to 8×4); resizing snaps to whole units; order and sizes saved per computer in the
  "deck" prefs and restored after an app restart.
- Verification command: `./gradlew testDebugUnitTest --tests '*DeckGridTest*'`
- Files likely touched: `DeckGrid.kt`
- Test seams: `DeckLayout.resize/move`, `encode/decode`
- Blocked by: 1.1

### Phase 2: the deck screen
Verified when: Deck Mode opens from Commands, shows the panels on the grid, opens apps and
sends media keys on the real Mac, and closes with Back or ✕.
Files to touch: `DeckMode.kt` (new), `MainActivity.kt`, `tools/strings/deck.py` (new), tests.

#### Step 2.1: Deck overlay + entry card (Size: S)
- Acceptance criteria: a "Deck Mode" card (first in Commands, reorderable, hidden when the
  computer supports neither `apps` nor `media`) opens a full-screen deck; Back and ✕ close it;
  the screen stays on and the system bars hide while it is open.
- Verification command: `./gradlew testDebugUnitTest --tests '*DeckModeTest*'`
- Test seams: `DeckScreen(model)` with a plain `DeckModel` (Robolectric)
- Blocked by: 1.2

#### Step 2.2: Favorites panel (Size: S)
- Acceptance criteria: the same favorite list as the Apps card; one app tile per grid unit
  (a 3×2 panel shows 6 favorites); tap opens the app.
- Test seams: `DeckScreen` click → `onOpenApp(bundleId)`
- Blocked by: 2.1

#### Step 2.3: Media panel (Size: S)
- Acceptance criteria: Previous, Play/Pause, Next and the volume slider (sent on finger lift);
  at 2×1 only the three keys and the slider, larger sizes give larger buttons.
- Test seams: `DeckScreen` click → `onMedia(kind, volume)`
- Blocked by: 2.1

#### Step 2.4: Quick presses don't wait (Size: S)
- Acceptance criteria: in the deck a press is sent even while the previous command is still
  pending (Next, Next, Next all arrive, in order); the status light follows the latest one.
- Test seams: the send queue as a pure helper + manual check on the Mac
- Blocked by: 2.2, 2.3

#### Step 2.5: Edit mode: drag to reorder, drag the corner to resize (Size: M)
- Acceptance criteria: an Edit button shows a corner handle on each panel; dragging the panel
  moves it in the order, dragging the handle resizes it unit by unit within its limits; Done
  saves.
- Verification command: Robolectric drag tests + pixel tests at `land` and `port`
- Blocked by: 2.4

## Decision log
| Decision | Chosen option | Why |
| --- | --- | --- |
| Where the deck lives | Full-screen overlay inside the main screen (like Settings) | Reuses the shared state and `sendCommand`; no second Activity |
| Orientation | Follows the phone | User choice |
| Grid | Whole screen = 8×4 units (portrait 4×8) | User choice |
| Panel size | User resizes, in whole units | User choice |
| Placement | User drags to reorder; panels flow into the first free spot | User choice; no gaps by construction |
| Favorites | Same list as the Apps card; one tile per unit | User choice |
| Now playing | Not in v1 | User choice; needs agent work |
| Agent changes | None | Media and app opening already exist |

## Decision log
| Decision | Chosen option | Why |
| --- | --- | --- |
| Where the deck lives | Full-screen overlay inside the main screen (like Settings) | Reuses the shared state and `sendCommand`; no second Activity, no state plumbing |
| Agent changes | None for v1 | Media and app opening already exist |
| Texts | EN/TR/NL from `tools/strings/deck.py` | Same rule as the rest of the app |

## Not yet specified
- Track title and artist ("Now playing"): later, its own plan (agent work on macOS).
- Future panels (which ones): fog, decided when each is requested; each declares its min/max size.

## Always / Never
- Always: one tap per action, large touch targets, every text in EN/TR/NL, version bump.
- Never: admin features in the deck; new agent capabilities in this plan unless approved below.

## Progress
| Phase | Status |
| --- | --- |
| 1 grid engine | not started |
| 2 deck screen | not started |
