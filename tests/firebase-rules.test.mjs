import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  equalTo,
  get,
  orderByChild,
  query,
  ref,
  serverTimestamp,
  set,
  update,
} from "firebase/database";

const ANDROID = "57HVnDSvZKNbJBabrDF4c2gnv462";
const PI = "N8V4nrLrELa83EXRLWJsiis85Bq1";
const OTHER = "different-authenticated-user";
const rules = await readFile(new URL("../database.rules.json", import.meta.url), "utf8");
const testEnv = await initializeTestEnvironment({
  projectId: "demo-comapp-av",
  database: {
    host: process.env.FIREBASE_DATABASE_EMULATOR_HOST?.split(":")[0] ?? "127.0.0.1",
    port: Number(process.env.FIREBASE_DATABASE_EMULATOR_HOST?.split(":")[1] ?? 9000),
    rules,
  },
});

let checks = 0;
async function check(label, fn) {
  await fn();
  checks++;
  process.stdout.write(`✓ ${label}\n`);
}

try {
  const anonymousDb = testEnv.unauthenticatedContext().database();
  const androidDb = testEnv.authenticatedContext(ANDROID).database();
  const piDb = testEnv.authenticatedContext(PI).database();
  const command = (db, id) => ref(db, `commands/${id}`);

  await check("unauthenticated read and write are denied", async () => {
    await assertFails(get(ref(anonymousDb, "commands")));
    await assertFails(set(command(anonymousDb, "unauth"), {
      action: "PING", senderUid: ANDROID, targetUid: PI, status: "PENDING",
    }));
  });

  await check("Android creates a PENDING PING and reads its own outcome", async () => {
    await assertSucceeds(set(command(androidDb, "android-owned"), {
      action: "PING", senderUid: ANDROID, targetUid: PI, status: "PENDING",
    }));
    const own = await assertSucceeds(get(command(androidDb, "android-owned")));
    assert.equal(own.val().status, "PENDING");
  });

  await check("Android cannot impersonate, submit terminal status, use another action, or add fields", async () => {
    const rejected = [
      ["forged-sender", { action: "PING", senderUid: OTHER, targetUid: PI, status: "PENDING" }],
      ["android-success", { action: "PING", senderUid: ANDROID, targetUid: PI, status: "SUCCESS", result: "PONG" }],
      ["android-failed", { action: "PING", senderUid: ANDROID, targetUid: PI, status: "FAILED", errorCode: "AGENT_ERROR" }],
      ["unknown-action", { action: "SHELL", senderUid: ANDROID, targetUid: PI, status: "PENDING" }],
      ["unexpected-field", { action: "PING", senderUid: ANDROID, targetUid: PI, status: "PENDING", shellCommand: "reboot" }],
    ];
    for (const [id, value] of rejected) await assertFails(set(command(androidDb, id), value));
    await assertFails(set(command(androidDb, "android-owned"), {
      action: "PING", senderUid: OTHER, targetUid: PI, status: "PENDING",
    }));
    await assertFails(update(command(androidDb, "android-owned"), { status: "SUCCESS", result: "PONG" }));
  });

  await check("Android reads its command only; Pi queries only commands addressed to it", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await set(ref(context.database(), "commands/other-owner"), {
        action: "PING", senderUid: OTHER, targetUid: PI, status: "PENDING",
      });
      await set(ref(context.database(), "commands/other-target"), {
        action: "PING", senderUid: OTHER, targetUid: "another-agent", status: "PENDING",
      });
    });
    await assertFails(get(command(androidDb, "other-owner")));
    const piCommands = await assertSucceeds(get(query(
      ref(piDb, "commands"),
      orderByChild("targetUid"),
      equalTo(PI),
    )));
    assert.ok(piCommands.child("android-owned").exists());
    assert.ok(piCommands.child("other-owner").exists());
    assert.equal(piCommands.child("other-target").exists(), false);
    await assertFails(get(ref(piDb, "commands")));
  });

  await check("Pi can claim, complete, fail, and recover stale PING commands", async () => {
    await assertFails(update(command(piDb, "android-owned"), { status: "SUCCESS", result: "PONG" }));
    await assertSucceeds(update(command(piDb, "android-owned"), {
      status: "PROCESSING", processingAt: Date.now(),
    }));
    await assertFails(update(command(piDb, "android-owned"), {
      status: "PROCESSING", processingAt: Date.now(),
    }));
    await assertSucceeds(update(command(piDb, "android-owned"), {
      status: "SUCCESS", result: "PONG", processingAt: null,
    }));
    const result = await assertSucceeds(get(command(androidDb, "android-owned")));
    assert.equal(result.val().status, "SUCCESS");
    assert.equal(result.val().result, "PONG");
    await assertFails(update(command(piDb, "android-owned"), {
      status: "PROCESSING", processingAt: Date.now(),
    }));

    await assertSucceeds(set(command(androidDb, "android-failed"), {
      action: "PING", senderUid: ANDROID, targetUid: PI, status: "PENDING",
    }));
    await assertSucceeds(update(command(piDb, "android-failed"), {
      status: "PROCESSING", processingAt: Date.now(),
    }));
    await assertSucceeds(update(command(piDb, "android-failed"), {
      status: "FAILED", errorCode: "AGENT_ERROR", processingAt: null,
    }));

    await testEnv.withSecurityRulesDisabled(async (context) => {
      await set(ref(context.database(), "commands/stale"), {
        action: "PING", senderUid: ANDROID, targetUid: PI, status: "PROCESSING",
        processingAt: Date.now() - 180_000,
      });
    });
    await assertSucceeds(update(command(piDb, "stale"), {
      status: "PROCESSING", processingAt: Date.now(),
    }));
  });

  await check("Pi cannot alter immutable fields, add fields, or delete commands", async () => {
    for (const patch of [
      { senderUid: OTHER },
      { targetUid: "another-agent" },
      { action: "SHELL" },
      { status: "FAILED", errorCode: "AGENT_ERROR", debug: "unexpected" },
    ]) {
      await assertFails(update(command(piDb, "android-owned"), patch));
    }
    await assertFails(set(command(piDb, "android-owned"), null));
    await assertFails(set(command(androidDb, "android-owned"), null));
    await assertFails(update(command(androidDb, "android-owned"), { status: "FAILED", errorCode: "AGENT_ERROR" }));
  });
  await check("SPEAK carries 1-300 characters of text and completes with SPOKEN", async () => {
    const speak = (text, extra = {}) => ({
      action: "SPEAK", senderUid: ANDROID, targetUid: PI, status: "PENDING", text, ...extra,
    });
    await assertSucceeds(set(command(androidDb, "speak-ok"), speak("Merhaba Ferhat")));
    await assertSucceeds(set(command(androidDb, "speak-max"), speak("a".repeat(300))));
    await assertFails(set(command(androidDb, "speak-empty"), speak("")));
    await assertFails(set(command(androidDb, "speak-long"), speak("a".repeat(301))));
    await assertFails(set(command(androidDb, "speak-notext"), {
      action: "SPEAK", senderUid: ANDROID, targetUid: PI, status: "PENDING",
    }));
    await assertFails(set(command(androidDb, "speak-number"), speak(42)));
    await assertFails(set(command(androidDb, "ping-with-text"), {
      action: "PING", senderUid: ANDROID, targetUid: PI, status: "PENDING", text: "hello",
    }));
    await assertFails(set(command(androidDb, "speak-extra"), speak("hi", { voice: "tr" })));
    await assertFails(set(command(androidDb, "speak-success"), speak("hi", { status: "SUCCESS", result: "SPOKEN" })));

    await assertSucceeds(update(command(piDb, "speak-ok"), {
      status: "PROCESSING", processingAt: Date.now(),
    }));
    await assertFails(update(command(piDb, "speak-ok"), { text: "changed" }));
    await assertFails(update(command(piDb, "speak-ok"), { status: "SUCCESS", result: "PONG", processingAt: null }));
    await assertSucceeds(update(command(piDb, "speak-ok"), {
      status: "SUCCESS", result: "SPOKEN", processingAt: null,
    }));
    const done = await assertSucceeds(get(command(androidDb, "speak-ok")));
    assert.equal(done.val().text, "Merhaba Ferhat");
    assert.equal(done.val().result, "SPOKEN");

    await assertSucceeds(update(command(piDb, "speak-max"), {
      status: "PROCESSING", processingAt: Date.now(),
    }));
    await assertSucceeds(update(command(piDb, "speak-max"), {
      status: "FAILED", errorCode: "AGENT_ERROR", processingAt: null,
    }));

    // PING must still finish with PONG only.
    await assertSucceeds(set(command(androidDb, "ping-again"), {
      action: "PING", senderUid: ANDROID, targetUid: PI, status: "PENDING",
    }));
    await assertSucceeds(update(command(piDb, "ping-again"), {
      status: "PROCESSING", processingAt: Date.now(),
    }));
    await assertFails(update(command(piDb, "ping-again"), { status: "SUCCESS", result: "SPOKEN", processingAt: null }));
  });
  await check("SLEEP needs a fresh server timestamp, carries no text, and completes with SLEEPING", async () => {
    const sleep = (extra = {}) => ({
      action: "SLEEP", senderUid: ANDROID, targetUid: PI, status: "PENDING",
      createdAt: serverTimestamp(), ...extra,
    });
    await assertSucceeds(set(command(androidDb, "sleep-ok"), sleep()));
    await assertFails(set(command(androidDb, "sleep-no-time"), {
      action: "SLEEP", senderUid: ANDROID, targetUid: PI, status: "PENDING",
    }));
    await assertFails(set(command(androidDb, "sleep-old"), sleep({ createdAt: Date.now() - 120_000 })));
    await assertFails(set(command(androidDb, "sleep-future"), sleep({ createdAt: Date.now() + 3_600_000 })));
    await assertFails(set(command(androidDb, "sleep-text"), sleep({ text: "hello" })));
    await assertFails(set(command(androidDb, "sleep-extra"), sleep({ delay: 5 })));
    await assertFails(set(command(androidDb, "ping-with-time"), {
      action: "PING", senderUid: ANDROID, targetUid: PI, status: "PENDING", createdAt: serverTimestamp(),
    }));
    await assertFails(set(command(androidDb, "speak-with-time"), {
      action: "SPEAK", senderUid: ANDROID, targetUid: PI, status: "PENDING", text: "hi",
      createdAt: serverTimestamp(),
    }));

    await assertSucceeds(update(command(piDb, "sleep-ok"), {
      status: "PROCESSING", processingAt: Date.now(),
    }));
    await assertFails(update(command(piDb, "sleep-ok"), { createdAt: Date.now() }));
    await assertFails(update(command(piDb, "sleep-ok"), { status: "SUCCESS", result: "PONG", processingAt: null }));
    await assertFails(update(command(piDb, "sleep-ok"), { status: "SUCCESS", result: "SPOKEN", processingAt: null }));
    await assertSucceeds(update(command(piDb, "sleep-ok"), {
      status: "SUCCESS", result: "SLEEPING", processingAt: null,
    }));
    const done = await assertSucceeds(get(command(androidDb, "sleep-ok")));
    assert.equal(done.val().result, "SLEEPING");
    assert.equal(typeof done.val().createdAt, "number");
  });
  await check("SLEEP accepts a whole number of minutes from 1 to 1440, only on SLEEP, and the Pi cannot change it", async () => {
    const timed = (durationMinutes) => ({
      action: "SLEEP", senderUid: ANDROID, targetUid: PI, status: "PENDING",
      createdAt: serverTimestamp(), durationMinutes,
    });
    for (const minutes of [1, 2, 80, 1440]) {
      await assertSucceeds(set(command(androidDb, `sleep-${minutes}`), timed(minutes)));
    }
    for (const [id, value] of [
      ["zero", 0], ["too-long", 1441], ["negative", -5], ["fraction", 1.5], ["string", "30"],
    ]) {
      await assertFails(set(command(androidDb, `sleep-bad-${id}`), timed(value)));
    }
    await assertFails(set(command(androidDb, "ping-with-duration"), {
      action: "PING", senderUid: ANDROID, targetUid: PI, status: "PENDING", durationMinutes: 5,
    }));
    await assertFails(set(command(androidDb, "speak-with-duration"), {
      action: "SPEAK", senderUid: ANDROID, targetUid: PI, status: "PENDING", text: "hi", durationMinutes: 5,
    }));

    await assertSucceeds(update(command(piDb, "sleep-80"), {
      status: "PROCESSING", processingAt: Date.now(),
    }));
    await assertFails(update(command(piDb, "sleep-80"), { durationMinutes: 600 }));
    await assertSucceeds(update(command(piDb, "sleep-80"), {
      status: "SUCCESS", result: "SLEEPING", processingAt: null,
    }));
    const done = await assertSucceeds(get(command(androidDb, "sleep-80")));
    assert.equal(done.val().durationMinutes, 80);
  });
  await check("SHUTDOWN needs a fresh server timestamp, carries no text/duration, and completes with POWERING_OFF", async () => {
    const shutdown = (extra = {}) => ({
      action: "SHUTDOWN", senderUid: ANDROID, targetUid: PI, status: "PENDING",
      createdAt: serverTimestamp(), ...extra,
    });
    await assertSucceeds(set(command(androidDb, "shutdown-ok"), shutdown()));
    await assertFails(set(command(androidDb, "shutdown-no-time"), {
      action: "SHUTDOWN", senderUid: ANDROID, targetUid: PI, status: "PENDING",
    }));
    await assertFails(set(command(androidDb, "shutdown-old"), shutdown({ createdAt: Date.now() - 120_000 })));
    await assertFails(set(command(androidDb, "shutdown-future"), shutdown({ createdAt: Date.now() + 3_600_000 })));
    await assertFails(set(command(androidDb, "shutdown-text"), shutdown({ text: "hello" })));
    await assertFails(set(command(androidDb, "shutdown-duration"), shutdown({ durationMinutes: 10 })));
    await assertFails(set(command(androidDb, "shutdown-extra"), shutdown({ delay: 5 })));

    await assertSucceeds(update(command(piDb, "shutdown-ok"), {
      status: "PROCESSING", processingAt: Date.now(),
    }));
    await assertFails(update(command(piDb, "shutdown-ok"), { createdAt: Date.now() }));
    await assertFails(update(command(piDb, "shutdown-ok"), { status: "SUCCESS", result: "SLEEPING", processingAt: null }));
    await assertFails(update(command(piDb, "shutdown-ok"), { status: "SUCCESS", result: "SCANNED", processingAt: null }));
    await assertSucceeds(update(command(piDb, "shutdown-ok"), {
      status: "SUCCESS", result: "POWERING_OFF", processingAt: null,
    }));
    const done = await assertSucceeds(get(command(androidDb, "shutdown-ok")));
    assert.equal(done.val().result, "POWERING_OFF");
    assert.equal(typeof done.val().createdAt, "number");

    // Other actions must still reject POWERING_OFF as their result.
    await assertSucceeds(set(command(androidDb, "scan-not-poweroff"), {
      action: "SCAN", senderUid: ANDROID, targetUid: PI, status: "PENDING",
    }));
    await assertSucceeds(update(command(piDb, "scan-not-poweroff"), {
      status: "PROCESSING", processingAt: Date.now(),
    }));
    await assertFails(update(command(piDb, "scan-not-poweroff"), { status: "SUCCESS", result: "POWERING_OFF", processingAt: null }));
  });
  await check("SCAN carries no text and completes with SCANNED", async () => {
    await assertSucceeds(set(command(androidDb, "scan-ok"), {
      action: "SCAN", senderUid: ANDROID, targetUid: PI, status: "PENDING",
    }));
    await assertFails(set(command(androidDb, "scan-text"), {
      action: "SCAN", senderUid: ANDROID, targetUid: PI, status: "PENDING", text: "hi",
    }));
    await assertFails(set(command(androidDb, "scan-success"), {
      action: "SCAN", senderUid: ANDROID, targetUid: PI, status: "SUCCESS", result: "SCANNED",
    }));

    await assertSucceeds(update(command(piDb, "scan-ok"), {
      status: "PROCESSING", processingAt: Date.now(),
    }));
    await assertFails(update(command(piDb, "scan-ok"), { status: "SUCCESS", result: "PONG", processingAt: null }));
    await assertFails(update(command(piDb, "scan-ok"), { status: "SUCCESS", result: "SPOKEN", processingAt: null }));
    await assertSucceeds(update(command(piDb, "scan-ok"), {
      status: "SUCCESS", result: "SCANNED", processingAt: null,
    }));
    const done = await assertSucceeds(get(command(androidDb, "scan-ok")));
    assert.equal(done.val().result, "SCANNED");

    // Other actions must still reject SCANNED as their result.
    await assertSucceeds(set(command(androidDb, "ping-not-scanned"), {
      action: "PING", senderUid: ANDROID, targetUid: PI, status: "PENDING",
    }));
    await assertSucceeds(update(command(piDb, "ping-not-scanned"), {
      status: "PROCESSING", processingAt: Date.now(),
    }));
    await assertFails(update(command(piDb, "ping-not-scanned"), { status: "SUCCESS", result: "SCANNED", processingAt: null }));
  });
  await check("POWER_MODE carries a mode (PERFORMANCE/POWERSAVE), no text, and completes with MODE_SET", async () => {
    await assertSucceeds(set(command(androidDb, "mode-save"), {
      action: "POWER_MODE", senderUid: ANDROID, targetUid: PI, status: "PENDING", mode: "POWERSAVE",
    }));
    await assertSucceeds(set(command(androidDb, "mode-perf"), {
      action: "POWER_MODE", senderUid: ANDROID, targetUid: PI, status: "PENDING", mode: "PERFORMANCE",
    }));
    await assertFails(set(command(androidDb, "mode-no-mode"), {
      action: "POWER_MODE", senderUid: ANDROID, targetUid: PI, status: "PENDING",
    }));
    await assertFails(set(command(androidDb, "mode-bad-value"), {
      action: "POWER_MODE", senderUid: ANDROID, targetUid: PI, status: "PENDING", mode: "TURBO",
    }));
    await assertFails(set(command(androidDb, "mode-text"), {
      action: "POWER_MODE", senderUid: ANDROID, targetUid: PI, status: "PENDING", mode: "POWERSAVE", text: "hi",
    }));
    await assertFails(set(command(androidDb, "ping-with-mode"), {
      action: "PING", senderUid: ANDROID, targetUid: PI, status: "PENDING", mode: "POWERSAVE",
    }));

    await assertSucceeds(update(command(piDb, "mode-save"), {
      status: "PROCESSING", processingAt: Date.now(),
    }));
    await assertFails(update(command(piDb, "mode-save"), { mode: "PERFORMANCE" }));
    await assertFails(update(command(piDb, "mode-save"), { status: "SUCCESS", result: "SCANNED", processingAt: null }));
    await assertSucceeds(update(command(piDb, "mode-save"), {
      status: "SUCCESS", result: "MODE_SET", processingAt: null,
    }));
    const done = await assertSucceeds(get(command(androidDb, "mode-save")));
    assert.equal(done.val().result, "MODE_SET");
    assert.equal(done.val().mode, "POWERSAVE");

    // Other actions must still reject MODE_SET as their result.
    await assertSucceeds(set(command(androidDb, "ping-not-mode-set"), {
      action: "PING", senderUid: ANDROID, targetUid: PI, status: "PENDING",
    }));
    await assertSucceeds(update(command(piDb, "ping-not-mode-set"), {
      status: "PROCESSING", processingAt: Date.now(),
    }));
    await assertFails(update(command(piDb, "ping-not-mode-set"), { status: "SUCCESS", result: "MODE_SET", processingAt: null }));
  });
  await check("DOWNLOAD_ADD: magnet/URL/torrent-file sources, upload first, specific error codes", async () => {
    const dl = (over) => ({ action: "DOWNLOAD_ADD", senderUid: ANDROID, targetUid: PI, status: "PENDING", ...over });
    await assertSucceeds(set(command(androidDb, "dl-magnet"), dl({ source: "MAGNET", url: "magnet:?xt=urn:btih:abc&dn=ubuntu" })));
    await assertSucceeds(set(command(androidDb, "dl-url"), dl({ source: "URL", url: "https://example.com/fedora.iso" })));
    await assertFails(set(command(androidDb, "dl-file-scheme"), dl({ source: "URL", url: "file:///etc/passwd" })));
    await assertFails(set(command(androidDb, "dl-magnet-not"), dl({ source: "MAGNET", url: "https://example.com/x" })));
    await assertFails(set(command(androidDb, "dl-no-source"), dl({ url: "https://example.com/x.iso" })));
    await assertFails(set(command(androidDb, "ping-with-url"), {
      action: "PING", senderUid: ANDROID, targetUid: PI, status: "PENDING", source: "URL", url: "https://example.com/x",
    }));

    // A torrent file is uploaded first; only then may its command exist.
    await assertFails(set(command(androidDb, "dl-torrent"), dl({ source: "TORRENT_FILE" })));
    await assertSucceeds(set(ref(androidDb, "uploads/dl-torrent"), { torrent: "ZDg6YW5ub3VuY2U=" }));
    await assertFails(set(ref(androidDb, "uploads/dl-torrent"), { torrent: "ZQ==" }));
    await assertFails(get(ref(androidDb, "uploads/dl-torrent")));
    await assertSucceeds(set(command(androidDb, "dl-torrent"), dl({ source: "TORRENT_FILE" })));
    const upload = await assertSucceeds(get(ref(piDb, "uploads/dl-torrent")));
    assert.equal(upload.val().torrent, "ZDg6YW5ub3VuY2U=");
    await assertSucceeds(set(ref(piDb, "uploads/dl-torrent"), null));

    await assertSucceeds(update(command(piDb, "dl-magnet"), { status: "PROCESSING", processingAt: Date.now() }));
    await assertFails(update(command(piDb, "dl-magnet"), { url: "magnet:?xt=urn:btih:zzz" }));
    await assertFails(update(command(piDb, "dl-magnet"), { status: "SUCCESS", result: "PONG", processingAt: null }));
    await assertSucceeds(update(command(piDb, "dl-magnet"), { status: "SUCCESS", result: "DOWNLOAD_STARTED", processingAt: null }));

    await assertSucceeds(update(command(piDb, "dl-url"), { status: "PROCESSING", processingAt: Date.now() }));
    await assertFails(update(command(piDb, "dl-url"), { status: "FAILED", errorCode: "WHATEVER", processingAt: null }));
    await assertSucceeds(update(command(piDb, "dl-url"), { status: "FAILED", errorCode: "NOT_A_FILE", processingAt: null }));

    // The download-only codes are not available to other actions.
    await assertSucceeds(set(command(androidDb, "ping-no-download-codes"), {
      action: "PING", senderUid: ANDROID, targetUid: PI, status: "PENDING",
    }));
    await assertSucceeds(update(command(piDb, "ping-no-download-codes"), { status: "PROCESSING", processingAt: Date.now() }));
    await assertFails(update(command(piDb, "ping-no-download-codes"), { status: "FAILED", errorCode: "NO_SPACE", processingAt: null }));
  });

  await check("Only the Pi writes /downloads/latest, Android reads it; only Android sets the speed limit", async () => {
    const report = {
      reportedAt: serverTimestamp(), appliedLimitMbps: 20,
      items: {
        "2089b05ecca3d829": {
          name: "ubuntu.iso", kind: "TORRENT", state: "DOWNLOADING",
          totalBytes: 6000, doneBytes: 1200, downBps: 2400, upBps: 10, etaSec: 2,
        },
      },
    };
    await assertFails(set(ref(androidDb, "downloads/latest"), report));
    await assertSucceeds(set(ref(piDb, "downloads/latest"), report));
    await assertFails(set(ref(piDb, "downloads/latest/items/2089b05ecca3d829/path"), "/home"));
    await assertSucceeds(get(ref(androidDb, "downloads/latest")));
    await assertFails(get(ref(piDb, "downloads/latest")));

    await assertSucceeds(set(ref(androidDb, "settings/downloadLimitMbps"), 20));
    await assertSucceeds(set(ref(androidDb, "settings/downloadLimitMbps"), 0));
    await assertFails(set(ref(androidDb, "settings/downloadLimitMbps"), -1));
    await assertFails(set(ref(androidDb, "settings/downloadLimitMbps"), 2.5));
    await assertFails(set(ref(piDb, "settings/downloadLimitMbps"), 50));
    await assertSucceeds(get(ref(piDb, "settings/downloadLimitMbps")));
  });

  await check("Only the Pi writes /network/latest, and only Android reads it, with required shape", async () => {
    await assertFails(get(ref(anonymousDb, "network/latest")));
    await assertFails(set(ref(piDb, "network/latest"), {
      scannedAt: Date.now(),
    }));
    await assertFails(set(ref(piDb, "network/latest"), {
      scannedAt: Date.now(),
      devices: { "AA-BB-CC-DD-EE-FF": { ip: "192.168.1.10" } },
    }));
    await assertFails(set(ref(piDb, "network/latest"), {
      scannedAt: Date.now(),
      devices: { "not-a-mac": { ip: "192.168.1.10", name: "Phone" } },
    }));
    await assertFails(set(ref(piDb, "network/latest"), {
      scannedAt: Date.now(),
      devices: { "AA-BB-CC-DD-EE-FF": { ip: "192.168.1.10", name: "Phone", note: "x" } },
    }));
    await assertFails(set(ref(androidDb, "network/latest"), {
      scannedAt: Date.now(),
      devices: {},
    }));

    await assertSucceeds(set(ref(piDb, "network/latest"), {
      scannedAt: Date.now(),
      devices: {
        "AA-BB-CC-DD-EE-FF": { ip: "192.168.1.10", name: "Fedora" },
        "11-22-33-44-55-66": { ip: "192.168.1.20", name: "Unknown" },
      },
    }));
    const latest = await assertSucceeds(get(ref(androidDb, "network/latest")));
    assert.equal(latest.child("devices/AA-BB-CC-DD-EE-FF/name").val(), "Fedora");
    await assertFails(get(ref(piDb, "network/latest")));
  });
  await check("Only the Pi writes /power/latest, and only Android reads it, with required shape", async () => {
    await assertFails(get(ref(anonymousDb, "power/latest")));
    // cpu is optional now - a reportedAt-only write is a valid (if useless) shape.
    await assertSucceeds(set(ref(piDb, "power/latest"), {
      reportedAt: Date.now(),
    }));
    await assertFails(set(ref(piDb, "power/latest"), {
      cpu: { usagePercent: 12.3 },
    }));
    await assertFails(set(ref(piDb, "power/latest"), {
      reportedAt: Date.now(),
      cpu: { usagePercent: 150 },
    }));
    await assertFails(set(ref(piDb, "power/latest"), {
      reportedAt: Date.now(),
      cpu: { tempC: 55, coreFreqMhz: { "cpu0": 1800 }, note: "x" },
    }));
    await assertFails(set(ref(piDb, "power/latest"), {
      reportedAt: Date.now(),
      cpu: { coreFreqMhz: { "notacore": 1800 } },
    }));
    await assertFails(set(ref(piDb, "power/latest"), {
      reportedAt: Date.now(),
      cpu: { usagePercent: 12.3 },
      battery: { percent: 150 },
    }));
    await assertFails(set(ref(piDb, "power/latest"), {
      reportedAt: Date.now(),
      cpu: { packageWatts: -1 },
    }));
    await assertFails(set(ref(piDb, "power/latest"), {
      reportedAt: Date.now(),
      cpu: { coreWatts: 4.1 }, // dropped feature: no sudoers rule, Rules no longer accept it
    }));
    await assertFails(set(ref(piDb, "power/latest"), {
      reportedAt: Date.now(),
      system: { fanRpm: 2400, ecTempC: 42, note: "x" },
    }));
    await assertFails(set(ref(piDb, "power/latest"), {
      reportedAt: Date.now(),
      system: { fanRpm: -5 },
    }));
    await assertSucceeds(set(ref(piDb, "power/latest"), {
      reportedAt: Date.now(),
      powerMode: "POWERSAVE",
    }));
    await assertFails(set(ref(piDb, "power/latest"), {
      reportedAt: Date.now(),
      powerMode: "TURBO",
    }));
    await assertFails(set(ref(androidDb, "power/latest"), {
      reportedAt: Date.now(),
      cpu: { usagePercent: 12.3 },
    }));

    await assertSucceeds(set(ref(piDb, "power/latest"), {
      reportedAt: Date.now(),
      cpu: {
        tempC: 55,
        usagePercent: 12.3,
        packageWatts: 9.4,
        coreFreqMhz: { "cpu0": 1800, "cpu1": 2200 },
      },
      battery: { percent: 87 },
      system: { fanRpm: 2400, ecTempC: 42 },
    }));
    await assertSucceeds(set(ref(piDb, "power/latest"), {
      reportedAt: Date.now(),
      cpu: { usagePercent: 5 },
    }));
    // cpu is optional now: a snapshot with only system/battery data is still valid shape.
    await assertSucceeds(set(ref(piDb, "power/latest"), {
      reportedAt: Date.now(),
      system: { fanRpm: 2200 },
    }));
    const latest = await assertSucceeds(get(ref(androidDb, "power/latest")));
    assert.equal(latest.child("system/fanRpm").val(), 2200);
    await assertFails(get(ref(piDb, "power/latest")));
  });
} finally {
  await testEnv.cleanup();
}

process.stdout.write(`PASS: ${checks} Firebase Realtime Database Rules test groups\n`);
