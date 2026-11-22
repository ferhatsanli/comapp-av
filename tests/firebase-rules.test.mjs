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
  limitToFirst,
  limitToLast,
  orderByChild,
  query,
  ref,
  serverTimestamp,
  set,
  update,
} from "firebase/database";

// One user (signed in with Google) with one paired computer, and strangers of both kinds.
const ANDROID = "user-android";
const PI = "agent-pi";
const OTHER = "other-user";
const OTHER_AGENT = "other-agent";
// The computer's entry in the user's list, and the tree with everything else about it.
const INFO = `users/${ANDROID}/devices/${PI}`;
const DEV = `users/${ANDROID}/data/${PI}`;
const GOOGLE = { firebase: { sign_in_provider: "google.com" } };
const ANON = { firebase: { sign_in_provider: "anonymous" } };
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
  const androidDb = testEnv.authenticatedContext(ANDROID, GOOGLE).database();
  const piDb = testEnv.authenticatedContext(PI, ANON).database();
  const otherDb = testEnv.authenticatedContext(OTHER, GOOGLE).database();
  const otherAgentDb = testEnv.authenticatedContext(OTHER_AGENT, ANON).database();
  const command = (db, id) => ref(db, `${DEV}/commands/${id}`);
  const seed = (path, value) => testEnv.withSecurityRulesDisabled((context) => set(ref(context.database(), path), value));
  const CODE = "ABCD2345";

  await check("Nothing is readable or writable outside the three trees, or without signing in", async () => {
    await assertFails(get(ref(anonymousDb, "/")));
    await assertFails(get(ref(androidDb, "/")));
    await assertFails(get(ref(androidDb, "users")));
    await assertFails(get(ref(androidDb, "agents")));
    await assertFails(set(ref(androidDb, "anything"), 1));
    await assertFails(get(ref(anonymousDb, `${DEV}/commands`)));
    await assertFails(set(command(anonymousDb, "unauth"), { action: "PING", status: "PENDING" }));
  });

  await check("Pairing: the agent publishes a code, a signed-in user who knows it claims the agent", async () => {
    const offer = { code: CODE, name: "MacBook Pro", platform: "macos", createdAt: serverTimestamp() };
    // Only the agent itself publishes its code, in the exact shape.
    await assertFails(set(ref(otherAgentDb, `pairing/${PI}`), offer));
    await assertFails(set(ref(piDb, `pairing/${PI}`), { ...offer, code: "abcd2345" }));
    await assertFails(set(ref(piDb, `pairing/${PI}`), { ...offer, code: "ABCD0OI1" }));
    await assertFails(set(ref(piDb, `pairing/${PI}`), { ...offer, platform: "amiga" }));
    await assertFails(set(ref(piDb, `pairing/${PI}`), { ...offer, createdAt: Date.now() - 3_600_000 }));
    await assertFails(set(ref(piDb, `pairing/${PI}`), { ...offer, shell: "x" }));
    await assertSucceeds(set(ref(piDb, `pairing/${PI}`), offer));
    await assertSucceeds(get(ref(piDb, `pairing/${PI}`)));

    // Codes can be looked up one at a time by signed-in users, never listed.
    const lookup = (db, code) => query(ref(db, "pairing"), orderByChild("code"), equalTo(code), limitToFirst(1));
    await assertFails(get(ref(androidDb, "pairing")));
    await assertFails(get(ref(androidDb, `pairing/${PI}`)));
    await assertFails(get(query(ref(androidDb, "pairing"), orderByChild("code"))));
    await assertFails(get(query(ref(androidDb, "pairing"), orderByChild("code"), equalTo(CODE))));
    await assertFails(get(lookup(otherAgentDb, CODE)));
    await assertFails(get(lookup(anonymousDb, CODE)));
    const found = await assertSucceeds(get(lookup(androidDb, CODE)));
    assert.equal(found.child(PI).child("name").val(), "MacBook Pro");
    assert.equal((await assertSucceeds(get(lookup(androidDb, "ZZZZ9999")))).exists(), false);

    // The device is inert until it is claimed.
    await assertFails(set(command(androidDb, "too-early"), { action: "PING", status: "PENDING" }));
    await assertFails(get(ref(piDb, `${DEV}/commands`)));
    await assertFails(set(ref(piDb, `${DEV}/power/latest`), { reportedAt: serverTimestamp() }));

    const claim = (uid, code) => ({
      [`agents/${PI}`]: { owner: uid, code, pairedAt: serverTimestamp() },
      [`users/${uid}/devices/${PI}`]: { name: "MacBook Pro", platform: "macos", pairedAt: serverTimestamp() },
      [`pairing/${PI}`]: null,
    });
    // Wrong code, someone else as owner, an anonymous account, or the info without the claim: no.
    await assertFails(update(ref(androidDb), claim(ANDROID, "ZZZZ9999")));
    await assertFails(update(ref(androidDb), claim(OTHER, CODE)));
    await assertFails(update(ref(otherAgentDb), claim(OTHER_AGENT, CODE)));
    await assertFails(set(ref(androidDb, `${INFO}`), { name: "x", platform: "macos", pairedAt: serverTimestamp() }));
    await assertFails(set(ref(androidDb, `pairing/${PI}`), null));
    await assertSucceeds(update(ref(androidDb), claim(ANDROID, CODE)));

    // Claimed once: the code is gone and nobody else can take the agent over.
    assert.equal((await assertSucceeds(get(ref(piDb, `pairing/${PI}`)))).exists(), false);
    await assertSucceeds(set(ref(piDb, `pairing/${PI}`), offer));
    await assertFails(update(ref(otherDb), claim(OTHER, CODE)));
    await assertFails(set(ref(otherDb, `agents/${PI}/owner`), OTHER));
    await assertSucceeds(set(ref(piDb, `pairing/${PI}`), null));
    assert.equal((await assertSucceeds(get(ref(piDb, `agents/${PI}/owner`)))).val(), ANDROID);
    await assertSucceeds(get(ref(androidDb, `agents/${PI}`)));
    await assertFails(get(ref(otherDb, `agents/${PI}`)));
  });

  await check("An expired code cannot be claimed", async () => {
    await seed(`pairing/${OTHER_AGENT}`, { code: "WXYZ6789", name: "old", platform: "linux", createdAt: Date.now() - 11 * 60_000 });
    await assertFails(update(ref(otherDb), {
      [`agents/${OTHER_AGENT}`]: { owner: OTHER, code: "WXYZ6789", pairedAt: serverTimestamp() },
      [`users/${OTHER}/devices/${OTHER_AGENT}`]: { name: "old", platform: "linux", pairedAt: serverTimestamp() },
    }));
    await seed(`pairing/${OTHER_AGENT}`, null);
  });

  await check("Device info: the user renames, the agent reports its facts, in a fixed shape", async () => {
    await assertSucceeds(set(ref(androidDb, `${INFO}/name`), "Living room Mac"));
    await assertFails(set(ref(androidDb, `${INFO}/name`), ""));
    await assertFails(set(ref(androidDb, `${INFO}/name`), "x".repeat(41)));
    await assertSucceeds(update(ref(piDb, `${INFO}`), {
      hostname: "ferhats-mbp", osVersion: "macOS 15.1", agentVersion: "2.0.0",
      capabilities: { speak: true, sleepCycle: true, systemUpdate: false },
    }));
    await assertFails(set(ref(piDb, `${INFO}/agentVersion`), "latest"));
    await assertFails(set(ref(piDb, `${INFO}/capabilities/speak`), "yes"));
    await assertFails(set(ref(piDb, `${INFO}/secret`), "x"));
    await assertFails(set(ref(piDb, `${INFO}/platform`), "beos"));
    await assertSucceeds(get(ref(androidDb, `users/${ANDROID}/devices`)));
    await assertSucceeds(get(ref(piDb, `${INFO}`)));
  });

  await check("The user creates a PENDING PING and reads its own outcome", async () => {
    await assertSucceeds(set(command(androidDb, "android-owned"), { action: "PING", status: "PENDING" }));
    const own = await assertSucceeds(get(command(androidDb, "android-owned")));
    assert.equal(own.val().status, "PENDING");
  });

  await check("The user cannot submit a terminal status, another action, or extra fields", async () => {
    const rejected = [
      ["android-success", { action: "PING", status: "SUCCESS", result: "PONG" }],
      ["android-failed", { action: "PING", status: "FAILED", errorCode: "AGENT_ERROR" }],
      ["unknown-action", { action: "SHELL", status: "PENDING" }],
      ["removed-action", { action: "FEDORA_TASKS", status: "PENDING" }],
      ["unexpected-field", { action: "PING", status: "PENDING", shellCommand: "reboot" }],
      ["old-fields", { action: "PING", status: "PENDING", senderUid: ANDROID, targetUid: PI }],
    ];
    for (const [id, value] of rejected) await assertFails(set(command(androidDb, id), value));
    await assertFails(update(command(androidDb, "android-owned"), { status: "SUCCESS", result: "PONG" }));
  });

  await check("Users and agents are isolated: no one touches another user's device", async () => {
    // Another user, another user's agent, and an anonymous account posing as a user.
    await assertFails(get(command(otherDb, "android-owned")));
    await assertFails(get(ref(otherDb, `users/${ANDROID}`)));
    await assertFails(get(ref(otherDb, `${INFO}`)));
    await assertFails(set(command(otherDb, "intruder"), { action: "SHUTDOWN", status: "PENDING", createdAt: serverTimestamp() }));
    await assertFails(set(ref(otherDb, `${INFO}/name`), "pwned"));
    await assertFails(get(ref(otherAgentDb, `${DEV}/commands`)));
    await assertFails(update(command(otherAgentDb, "android-owned"), { status: "PROCESSING", processingAt: Date.now() }));
    await assertFails(set(ref(otherAgentDb, `${DEV}/power/latest`), { reportedAt: serverTimestamp() }));
    const fakeUser = testEnv.authenticatedContext(ANDROID, ANON).database();
    await assertFails(get(ref(fakeUser, `${INFO}`)));
    await assertFails(set(command(fakeUser, "anon-user"), { action: "PING", status: "PENDING" }));
    // A user cannot conjure a device they have not paired, even under their own account.
    await assertFails(set(ref(otherDb, `users/${OTHER}/devices/${PI}`), { name: "x", platform: "macos", pairedAt: serverTimestamp() }));
    await assertFails(set(ref(otherDb, `users/${OTHER}/data/${PI}/commands/x`), { action: "PING", status: "PENDING" }));
    // The agent reads all of its commands; it serves only its owner.
    const all = await assertSucceeds(get(ref(piDb, `${DEV}/commands`)));
    assert.ok(all.child("android-owned").exists());
    await assertFails(get(ref(piDb, `users/${OTHER}/data/${PI}/commands`)));
    await assertFails(get(ref(piDb, `users/${ANDROID}/devices`)));
    await assertFails(get(ref(piDb, `users/${ANDROID}/data`)));
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
      action: "PING", status: "PENDING",
    }));
    await assertSucceeds(update(command(piDb, "android-failed"), {
      status: "PROCESSING", processingAt: Date.now(),
    }));
    await assertSucceeds(update(command(piDb, "android-failed"), {
      status: "FAILED", errorCode: "AGENT_ERROR", processingAt: null,
    }));

    await testEnv.withSecurityRulesDisabled(async (context) => {
      await set(ref(context.database(), `${DEV}/commands/stale`), {
        action: "PING", status: "PROCESSING",
        processingAt: Date.now() - 180_000,
      });
    });
    await assertSucceeds(update(command(piDb, "stale"), {
      status: "PROCESSING", processingAt: Date.now(),
    }));
  });

  await check("The agent cannot alter the request or add fields; finished commands can be cleared", async () => {
    for (const patch of [
      { action: "SHELL" },
      { action: "SHUTDOWN" },
      { status: "FAILED", errorCode: "AGENT_ERROR", debug: "unexpected" },
    ]) {
      await assertFails(update(command(piDb, "android-owned"), patch));
    }
    await assertFails(update(command(androidDb, "android-owned"), { status: "FAILED", errorCode: "AGENT_ERROR" }));
    // The agent prunes what is finished, never what is still waiting; the user clears anything.
    await assertSucceeds(set(command(androidDb, "waiting"), { action: "PING", status: "PENDING" }));
    await assertFails(set(command(piDb, "waiting"), null));
    await assertFails(set(command(otherDb, "waiting"), null));
    await assertSucceeds(set(command(androidDb, "waiting"), null));
    await assertSucceeds(set(command(piDb, "android-failed"), null));
    // NOT_SUPPORTED is open to every action (a platform that lacks the feature).
    await assertSucceeds(set(command(androidDb, "unsupported"), { action: "SCAN", status: "PENDING" }));
    await assertSucceeds(update(command(piDb, "unsupported"), { status: "PROCESSING", processingAt: Date.now() }));
    await assertSucceeds(update(command(piDb, "unsupported"), { status: "FAILED", errorCode: "NOT_SUPPORTED", processingAt: null }));
  });

  await check("SPEAK carries 1-300 characters of text and completes with SPOKEN", async () => {
    const speak = (text, extra = {}) => ({
      action: "SPEAK", status: "PENDING", text, ...extra,
    });
    await assertSucceeds(set(command(androidDb, "speak-ok"), speak("Merhaba Ferhat")));
    await assertSucceeds(set(command(androidDb, "speak-max"), speak("a".repeat(300))));
    await assertFails(set(command(androidDb, "speak-empty"), speak("")));
    await assertFails(set(command(androidDb, "speak-long"), speak("a".repeat(301))));
    await assertFails(set(command(androidDb, "speak-notext"), {
      action: "SPEAK", status: "PENDING",
    }));
    await assertFails(set(command(androidDb, "speak-number"), speak(42)));
    await assertFails(set(command(androidDb, "ping-with-text"), {
      action: "PING", status: "PENDING", text: "hello",
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
      action: "PING", status: "PENDING",
    }));
    await assertSucceeds(update(command(piDb, "ping-again"), {
      status: "PROCESSING", processingAt: Date.now(),
    }));
    await assertFails(update(command(piDb, "ping-again"), { status: "SUCCESS", result: "SPOKEN", processingAt: null }));
  });
  await check("SLEEP needs a fresh server timestamp, carries no text, and completes with SLEEPING", async () => {
    const sleep = (extra = {}) => ({
      action: "SLEEP", status: "PENDING",
      createdAt: serverTimestamp(), ...extra,
    });
    await assertSucceeds(set(command(androidDb, "sleep-ok"), sleep()));
    await assertFails(set(command(androidDb, "sleep-no-time"), {
      action: "SLEEP", status: "PENDING",
    }));
    await assertFails(set(command(androidDb, "sleep-old"), sleep({ createdAt: Date.now() - 120_000 })));
    await assertFails(set(command(androidDb, "sleep-future"), sleep({ createdAt: Date.now() + 3_600_000 })));
    await assertFails(set(command(androidDb, "sleep-text"), sleep({ text: "hello" })));
    await assertFails(set(command(androidDb, "sleep-extra"), sleep({ delay: 5 })));
    await assertFails(set(command(androidDb, "ping-with-time"), {
      action: "PING", status: "PENDING", createdAt: serverTimestamp(),
    }));
    await assertFails(set(command(androidDb, "speak-with-time"), {
      action: "SPEAK", status: "PENDING", text: "hi",
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
      action: "SLEEP", status: "PENDING",
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
      action: "PING", status: "PENDING", durationMinutes: 5,
    }));
    await assertFails(set(command(androidDb, "speak-with-duration"), {
      action: "SPEAK", status: "PENDING", text: "hi", durationMinutes: 5,
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
      action: "SHUTDOWN", status: "PENDING",
      createdAt: serverTimestamp(), ...extra,
    });
    await assertSucceeds(set(command(androidDb, "shutdown-ok"), shutdown()));
    await assertFails(set(command(androidDb, "shutdown-no-time"), {
      action: "SHUTDOWN", status: "PENDING",
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
      action: "SCAN", status: "PENDING",
    }));
    await assertSucceeds(update(command(piDb, "scan-not-poweroff"), {
      status: "PROCESSING", processingAt: Date.now(),
    }));
    await assertFails(update(command(piDb, "scan-not-poweroff"), { status: "SUCCESS", result: "POWERING_OFF", processingAt: null }));
  });
  await check("SCAN carries no text and completes with SCANNED", async () => {
    await assertSucceeds(set(command(androidDb, "scan-ok"), {
      action: "SCAN", status: "PENDING",
    }));
    await assertFails(set(command(androidDb, "scan-text"), {
      action: "SCAN", status: "PENDING", text: "hi",
    }));
    await assertFails(set(command(androidDb, "scan-success"), {
      action: "SCAN", status: "SUCCESS", result: "SCANNED",
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
      action: "PING", status: "PENDING",
    }));
    await assertSucceeds(update(command(piDb, "ping-not-scanned"), {
      status: "PROCESSING", processingAt: Date.now(),
    }));
    await assertFails(update(command(piDb, "ping-not-scanned"), { status: "SUCCESS", result: "SCANNED", processingAt: null }));
  });
  await check("POWER_MODE carries a mode (PERFORMANCE/POWERSAVE), no text, and completes with MODE_SET", async () => {
    await assertSucceeds(set(command(androidDb, "mode-save"), {
      action: "POWER_MODE", status: "PENDING", mode: "POWERSAVE",
    }));
    await assertSucceeds(set(command(androidDb, "mode-perf"), {
      action: "POWER_MODE", status: "PENDING", mode: "PERFORMANCE",
    }));
    await assertFails(set(command(androidDb, "mode-no-mode"), {
      action: "POWER_MODE", status: "PENDING",
    }));
    await assertFails(set(command(androidDb, "mode-bad-value"), {
      action: "POWER_MODE", status: "PENDING", mode: "TURBO",
    }));
    await assertFails(set(command(androidDb, "mode-text"), {
      action: "POWER_MODE", status: "PENDING", mode: "POWERSAVE", text: "hi",
    }));
    await assertFails(set(command(androidDb, "ping-with-mode"), {
      action: "PING", status: "PENDING", mode: "POWERSAVE",
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
      action: "PING", status: "PENDING",
    }));
    await assertSucceeds(update(command(piDb, "ping-not-mode-set"), {
      status: "PROCESSING", processingAt: Date.now(),
    }));
    await assertFails(update(command(piDb, "ping-not-mode-set"), { status: "SUCCESS", result: "MODE_SET", processingAt: null }));
  });
  await check("SPEAK may carry language tr/en (only SPEAK), and the agent can't change it", async () => {
    const speak = (over) => ({ action: "SPEAK", status: "PENDING", text: "hi", ...over });
    await assertSucceeds(set(command(androidDb, "sp-none"), speak({})));
    await assertSucceeds(set(command(androidDb, "sp-tr"), speak({ language: "tr" })));
    await assertSucceeds(set(command(androidDb, "sp-en"), speak({ language: "en" })));
    await assertFails(set(command(androidDb, "sp-de"), speak({ language: "de" })));
    await assertFails(set(command(androidDb, "ping-lang"), {
      action: "PING", status: "PENDING", language: "en",
    }));
    await assertFails(update(command(piDb, "sp-en"), { status: "PROCESSING", processingAt: Date.now(), language: "tr" }));
    await assertSucceeds(update(command(piDb, "sp-en"), { status: "PROCESSING", processingAt: Date.now() }));
    await assertSucceeds(update(command(piDb, "sp-en"), { status: "SUCCESS", result: "SPOKEN", processingAt: null }));
    const done = await assertSucceeds(get(command(androidDb, "sp-en")));
    assert.equal(done.val().language, "en");
  });
  await check("The user reads the device's commands (latest 20 in the app); other users cannot", async () => {
    await assertSucceeds(set(command(androidDb, "zz-recent-1"), { action: "PING", status: "PENDING" }));
    const latest = (db, limit) => query(ref(db, `${DEV}/commands`), limitToLast(limit));
    // Keys sort by name here (the app's push IDs sort by time), so this one is the newest.
    const listed = await assertSucceeds(get(latest(androidDb, 20)));
    assert.ok(listed.child("zz-recent-1").exists());
    await assertSucceeds(get(ref(androidDb, `${DEV}/commands`)));
    await assertFails(get(latest(otherDb, 5)));
    await assertFails(get(latest(otherAgentDb, 5)));
  });
  await check("SYSTEM_INFO has no extra fields and completes with REPORTED", async () => {
    await assertSucceeds(set(command(androidDb, "info-ok"), {
      action: "SYSTEM_INFO", status: "PENDING",
    }));
    await assertFails(set(command(androidDb, "info-text"), {
      action: "SYSTEM_INFO", status: "PENDING", text: "hi",
    }));
    await assertFails(set(command(androidDb, "info-mode"), {
      action: "SYSTEM_INFO", status: "PENDING", mode: "POWERSAVE",
    }));
    await assertSucceeds(update(command(piDb, "info-ok"), { status: "PROCESSING", processingAt: Date.now() }));
    await assertFails(update(command(piDb, "info-ok"), { status: "SUCCESS", result: "PONG", processingAt: null }));
    await assertSucceeds(update(command(piDb, "info-ok"), { status: "SUCCESS", result: "REPORTED", processingAt: null }));

    // Other actions must still reject REPORTED as their result.
    await assertSucceeds(set(command(androidDb, "ping-not-reported"), {
      action: "PING", status: "PENDING",
    }));
    await assertSucceeds(update(command(piDb, "ping-not-reported"), { status: "PROCESSING", processingAt: Date.now() }));
    await assertFails(update(command(piDb, "ping-not-reported"), { status: "SUCCESS", result: "REPORTED", processingAt: null }));
  });
  await check("DOWNLOAD_ADD: magnet/URL/torrent-file sources, upload first, specific error codes", async () => {
    const dl = (over) => ({ action: "DOWNLOAD_ADD", status: "PENDING", ...over });
    await assertSucceeds(set(command(androidDb, "dl-magnet"), dl({ source: "MAGNET", url: "magnet:?xt=urn:btih:abc&dn=ubuntu" })));
    await assertSucceeds(set(command(androidDb, "dl-url"), dl({ source: "URL", url: "https://example.com/fedora.iso" })));
    await assertFails(set(command(androidDb, "dl-file-scheme"), dl({ source: "URL", url: "file:///etc/passwd" })));
    await assertFails(set(command(androidDb, "dl-magnet-not"), dl({ source: "MAGNET", url: "https://example.com/x" })));
    await assertFails(set(command(androidDb, "dl-no-source"), dl({ url: "https://example.com/x.iso" })));
    await assertFails(set(command(androidDb, "ping-with-url"), {
      action: "PING", status: "PENDING", source: "URL", url: "https://example.com/x",
    }));

    // A torrent file is uploaded first; only then may its command exist.
    await assertFails(set(command(androidDb, "dl-torrent"), dl({ source: "TORRENT_FILE" })));
    await assertSucceeds(set(ref(androidDb, `${DEV}/uploads/dl-torrent`), { torrent: "ZDg6YW5ub3VuY2U=" }));
    await assertFails(set(ref(androidDb, `${DEV}/uploads/dl-torrent`), { torrent: "ZQ==" }));
    await assertFails(get(ref(otherDb, `${DEV}/uploads/dl-torrent`)));
    await assertSucceeds(set(command(androidDb, "dl-torrent"), dl({ source: "TORRENT_FILE" })));
    const upload = await assertSucceeds(get(ref(piDb, `${DEV}/uploads/dl-torrent`)));
    assert.equal(upload.val().torrent, "ZDg6YW5ub3VuY2U=");
    await assertSucceeds(set(ref(piDb, `${DEV}/uploads/dl-torrent`), null));

    await assertSucceeds(update(command(piDb, "dl-magnet"), { status: "PROCESSING", processingAt: Date.now() }));
    await assertFails(update(command(piDb, "dl-magnet"), { url: "magnet:?xt=urn:btih:zzz" }));
    await assertFails(update(command(piDb, "dl-magnet"), { status: "SUCCESS", result: "PONG", processingAt: null }));
    await assertSucceeds(update(command(piDb, "dl-magnet"), { status: "SUCCESS", result: "DOWNLOAD_STARTED", processingAt: null }));

    await assertSucceeds(update(command(piDb, "dl-url"), { status: "PROCESSING", processingAt: Date.now() }));
    await assertFails(update(command(piDb, "dl-url"), { status: "FAILED", errorCode: "WHATEVER", processingAt: null }));
    await assertSucceeds(update(command(piDb, "dl-url"), { status: "FAILED", errorCode: "NOT_A_FILE", processingAt: null }));

    // The download-only codes are not available to other actions.
    await assertSucceeds(set(command(androidDb, "ping-no-download-codes"), {
      action: "PING", status: "PENDING",
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
    await assertFails(set(ref(androidDb, `${DEV}/downloads/latest`), report));
    await assertSucceeds(set(ref(piDb, `${DEV}/downloads/latest`), report));
    await assertFails(set(ref(piDb, `${DEV}/downloads/latest/items/2089b05ecca3d829/path`), "/home"));
    await assertSucceeds(get(ref(androidDb, `${DEV}/downloads/latest`)));
    await assertFails(get(ref(piDb, `${DEV}/downloads/latest`)));

    await assertSucceeds(set(ref(androidDb, `${DEV}/settings/downloadLimitMbps`), 20));
    await assertSucceeds(set(ref(androidDb, `${DEV}/settings/downloadLimitMbps`), 0));
    await assertFails(set(ref(androidDb, `${DEV}/settings/downloadLimitMbps`), -1));
    await assertFails(set(ref(androidDb, `${DEV}/settings/downloadLimitMbps`), 2.5));
    await assertFails(set(ref(piDb, `${DEV}/settings/downloadLimitMbps`), 50));
    await assertSucceeds(get(ref(piDb, `${DEV}/settings/downloadLimitMbps`)));
  });

  await check("TAILSCALE carries state UP/DOWN and SYSTEM_UPDATE nothing; their own results and errors", async () => {
    const cmd = (action, over) => ({ action, status: "PENDING", ...over });
    await assertSucceeds(set(command(androidDb, "ts-up"), cmd("TAILSCALE", { state: "UP" })));
    await assertFails(set(command(androidDb, "ts-none"), cmd("TAILSCALE")));
    await assertFails(set(command(androidDb, "ts-on"), cmd("TAILSCALE", { state: "ON" })));
    await assertFails(set(command(androidDb, "ping-state"), cmd("PING", { state: "UP" })));
    await assertFails(update(command(piDb, "ts-up"), { status: "PROCESSING", processingAt: Date.now(), state: "DOWN" }));
    await assertSucceeds(update(command(piDb, "ts-up"), { status: "PROCESSING", processingAt: Date.now() }));
    await assertFails(update(command(piDb, "ts-up"), { status: "FAILED", errorCode: "UPDATE_RUNNING", processingAt: null }));
    await assertSucceeds(update(command(piDb, "ts-up"), { status: "SUCCESS", result: "TAILSCALE_SET", processingAt: null }));

    await assertSucceeds(set(command(androidDb, "upd"), cmd("SYSTEM_UPDATE")));
    await assertFails(set(command(androidDb, "upd-text"), cmd("SYSTEM_UPDATE", { text: "x" })));
    await assertSucceeds(update(command(piDb, "upd"), { status: "PROCESSING", processingAt: Date.now() }));
    await assertFails(update(command(piDb, "upd"), { status: "SUCCESS", result: "PONG", processingAt: null }));
    await assertSucceeds(update(command(piDb, "upd"), { status: "SUCCESS", result: "UPDATE_STARTED", processingAt: null }));

    await assertSucceeds(set(command(androidDb, "upd-busy"), cmd("SYSTEM_UPDATE")));
    await assertSucceeds(update(command(piDb, "upd-busy"), { status: "PROCESSING", processingAt: Date.now() }));
    await assertSucceeds(update(command(piDb, "upd-busy"), { status: "FAILED", errorCode: "UPDATE_RUNNING", processingAt: null }));
  });

  await check("Power save sleep cycle: Pi writes /power/cycle, Android sets the interval, SLEEP_CYCLE_ACTIVE", async () => {
    await assertSucceeds(set(ref(piDb, `${DEV}/power/cycle`), { state: "AWAKE", intervalMinutes: 20, updatedAt: Date.now() }));
    await assertSucceeds(set(ref(piDb, `${DEV}/power/cycle`), { state: "AWAKE", intervalMinutes: 20, hold: "SESSION", updatedAt: Date.now() }));
    await assertSucceeds(set(ref(piDb, `${DEV}/power/cycle`), {
      state: "SLEEPING", intervalMinutes: 30, nextWakeAt: Date.now() + 1_800_000, updatedAt: Date.now(),
    }));
    await assertSucceeds(set(ref(piDb, `${DEV}/power/cycle`), { state: "OFF", intervalMinutes: 20, updatedAt: Date.now() }));
    await assertFails(set(ref(piDb, `${DEV}/power/cycle`), { state: "AWAKE", intervalMinutes: 15, updatedAt: Date.now() }));
    await assertFails(set(ref(piDb, `${DEV}/power/cycle`), { state: "DOZING", intervalMinutes: 20, updatedAt: Date.now() }));
    await assertFails(set(ref(piDb, `${DEV}/power/cycle`), { state: "AWAKE", intervalMinutes: 20, nextWakeAt: Date.now(), updatedAt: Date.now() }));
    await assertFails(set(ref(piDb, `${DEV}/power/cycle`), { state: "SLEEPING", intervalMinutes: 20, hold: "SESSION", updatedAt: Date.now() }));
    await assertFails(set(ref(piDb, `${DEV}/power/cycle`), { state: "AWAKE", intervalMinutes: 20, hold: "BORED", updatedAt: Date.now() }));
    await assertSucceeds(set(ref(piDb, `${DEV}/power/cycle`), {
      state: "AWAKE", intervalMinutes: 10, hold: "SESSION", detail: "session 4: ferhat via sshd on pts/0", updatedAt: Date.now(),
    }));
    await assertFails(set(ref(piDb, `${DEV}/power/cycle`), { state: "AWAKE", intervalMinutes: 10, detail: "no hold", updatedAt: Date.now() }));
    await assertFails(set(ref(piDb, `${DEV}/power/cycle`), {
      state: "AWAKE", intervalMinutes: 10, hold: "SESSION", detail: "x".repeat(161), updatedAt: Date.now(),
    }));
    await assertFails(set(ref(piDb, `${DEV}/power/cycle`), { state: "AWAKE", intervalMinutes: 20, updatedAt: Date.now(), extra: 1 }));
    await assertFails(set(ref(piDb, `${DEV}/power/cycle`), { state: "AWAKE", intervalMinutes: 20 }));
    await assertFails(set(ref(androidDb, `${DEV}/power/cycle`), { state: "OFF", intervalMinutes: 20, updatedAt: Date.now() }));
    await assertSucceeds(get(ref(androidDb, `${DEV}/power/cycle`)));

    for (const minutes of [10, 20, 30, 60]) {
      await assertSucceeds(set(ref(androidDb, `${DEV}/settings/sleepIntervalMinutes`), minutes));
    }
    await assertFails(set(ref(androidDb, `${DEV}/settings/sleepIntervalMinutes`), 15));
    await assertFails(set(ref(androidDb, `${DEV}/settings/sleepIntervalMinutes`), "20"));
    await assertFails(set(ref(piDb, `${DEV}/settings/sleepIntervalMinutes`), 20));
    await assertSucceeds(get(ref(piDb, `${DEV}/settings/sleepIntervalMinutes`)));

    const sleep = { action: "SLEEP", status: "PENDING", createdAt: serverTimestamp() };
    await assertSucceeds(set(command(androidDb, "sleep-cycle"), sleep));
    await assertSucceeds(update(command(piDb, "sleep-cycle"), { status: "PROCESSING", processingAt: Date.now() }));
    await assertSucceeds(update(command(piDb, "sleep-cycle"), { status: "FAILED", errorCode: "SLEEP_CYCLE_ACTIVE", processingAt: null }));
    await assertSucceeds(set(command(androidDb, "ping-cycle"), { action: "PING", status: "PENDING" }));
    await assertSucceeds(update(command(piDb, "ping-cycle"), { status: "PROCESSING", processingAt: Date.now() }));
    await assertFails(update(command(piDb, "ping-cycle"), { status: "FAILED", errorCode: "SLEEP_CYCLE_ACTIVE", processingAt: null }));
  });

  await check("Pi keeps /power/history and /system/update; Android reads both", async () => {
    const key = String(Date.now());
    await assertSucceeds(set(ref(piDb, `${DEV}/power/history/${key}`), { cpu: 12.5, w: 7.1 }));
    await assertFails(set(ref(piDb, `${DEV}/power/history/abc`), { cpu: 1 }));
    await assertFails(set(ref(piDb, `${DEV}/power/history/${key}`), { cpu: 1, tempC: 40 }));
    await assertFails(set(ref(androidDb, `${DEV}/power/history/${key}`), { cpu: 1 }));
    await assertSucceeds(get(ref(androidDb, `${DEV}/power/history`)));
    await assertSucceeds(set(ref(piDb, `${DEV}/power/history/${key}`), null));

    await assertSucceeds(set(ref(piDb, `${DEV}/system/update`), {
      state: "RUNNING", phase: "INSTALLING", percent: 60, done: 3, total: 6, packages: 3, startedAt: Date.now(),
    }));
    await assertFails(set(ref(piDb, `${DEV}/system/update`), { state: "RUNNING", phase: "X", percent: 1 }));
    await assertFails(set(ref(androidDb, `${DEV}/system/update`), { state: "RUNNING", phase: "INSTALLING", percent: 1 }));
    await assertSucceeds(get(ref(androidDb, `${DEV}/system/update`)));

    await assertSucceeds(set(ref(piDb, `${DEV}/power/latest`), { reportedAt: Date.now(), tailscale: "RUNNING" }));
    await assertFails(set(ref(piDb, `${DEV}/power/latest`), { reportedAt: Date.now(), tailscale: "ON" }));
  });

  await check("Only the Pi writes /network/latest, and only Android reads it, with required shape", async () => {
    await assertFails(get(ref(anonymousDb, `${DEV}/network/latest`)));
    await assertFails(set(ref(piDb, `${DEV}/network/latest`), {
      scannedAt: Date.now(),
    }));
    await assertFails(set(ref(piDb, `${DEV}/network/latest`), {
      scannedAt: Date.now(),
      devices: { "AA-BB-CC-DD-EE-FF": { ip: "192.168.1.10" } },
    }));
    await assertFails(set(ref(piDb, `${DEV}/network/latest`), {
      scannedAt: Date.now(),
      devices: { "not-a-mac": { ip: "192.168.1.10", name: "Phone" } },
    }));
    await assertFails(set(ref(piDb, `${DEV}/network/latest`), {
      scannedAt: Date.now(),
      devices: { "AA-BB-CC-DD-EE-FF": { ip: "192.168.1.10", name: "Phone", note: "x" } },
    }));
    await assertFails(set(ref(androidDb, `${DEV}/network/latest`), {
      scannedAt: Date.now(),
      devices: {},
    }));

    await assertSucceeds(set(ref(piDb, `${DEV}/network/latest`), {
      scannedAt: Date.now(),
      devices: {
        "AA-BB-CC-DD-EE-FF": { ip: "192.168.1.10", name: "Fedora" },
        "11-22-33-44-55-66": { ip: "192.168.1.20", name: "Unknown" },
      },
    }));
    const latest = await assertSucceeds(get(ref(androidDb, `${DEV}/network/latest`)));
    assert.equal(latest.child("devices/AA-BB-CC-DD-EE-FF/name").val(), "Fedora");
    await assertFails(get(ref(piDb, `${DEV}/network/latest`)));
  });
  await check("Only the Pi writes /power/latest, and only Android reads it, with required shape", async () => {
    await assertFails(get(ref(anonymousDb, `${DEV}/power/latest`)));
    // cpu is optional now - a reportedAt-only write is a valid (if useless) shape.
    await assertSucceeds(set(ref(piDb, `${DEV}/power/latest`), {
      reportedAt: Date.now(),
    }));
    await assertFails(set(ref(piDb, `${DEV}/power/latest`), {
      cpu: { usagePercent: 12.3 },
    }));
    await assertFails(set(ref(piDb, `${DEV}/power/latest`), {
      reportedAt: Date.now(),
      cpu: { usagePercent: 150 },
    }));
    await assertFails(set(ref(piDb, `${DEV}/power/latest`), {
      reportedAt: Date.now(),
      cpu: { tempC: 55, coreFreqMhz: { "cpu0": 1800 }, note: "x" },
    }));
    await assertFails(set(ref(piDb, `${DEV}/power/latest`), {
      reportedAt: Date.now(),
      cpu: { coreFreqMhz: { "notacore": 1800 } },
    }));
    await assertFails(set(ref(piDb, `${DEV}/power/latest`), {
      reportedAt: Date.now(),
      cpu: { usagePercent: 12.3 },
      battery: { percent: 150 },
    }));
    await assertFails(set(ref(piDb, `${DEV}/power/latest`), {
      reportedAt: Date.now(),
      cpu: { packageWatts: -1 },
    }));
    await assertFails(set(ref(piDb, `${DEV}/power/latest`), {
      reportedAt: Date.now(),
      cpu: { coreWatts: 4.1 }, // dropped feature: no sudoers rule, Rules no longer accept it
    }));
    await assertFails(set(ref(piDb, `${DEV}/power/latest`), {
      reportedAt: Date.now(),
      system: { fanRpm: 2400, ecTempC: 42, note: "x" },
    }));
    await assertFails(set(ref(piDb, `${DEV}/power/latest`), {
      reportedAt: Date.now(),
      system: { fanRpm: -5 },
    }));
    await assertSucceeds(set(ref(piDb, `${DEV}/power/latest`), {
      reportedAt: Date.now(),
      powerMode: "POWERSAVE",
    }));
    await assertFails(set(ref(piDb, `${DEV}/power/latest`), {
      reportedAt: Date.now(),
      powerMode: "TURBO",
    }));
    await assertFails(set(ref(androidDb, `${DEV}/power/latest`), {
      reportedAt: Date.now(),
      cpu: { usagePercent: 12.3 },
    }));

    await assertSucceeds(set(ref(piDb, `${DEV}/power/latest`), {
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
    await assertSucceeds(set(ref(piDb, `${DEV}/power/latest`), {
      reportedAt: Date.now(),
      cpu: { usagePercent: 5 },
    }));
    // cpu is optional now: a snapshot with only system/battery data is still valid shape.
    await assertSucceeds(set(ref(piDb, `${DEV}/power/latest`), {
      reportedAt: Date.now(),
      system: { fanRpm: 2200 },
    }));
    const latest = await assertSucceeds(get(ref(androidDb, `${DEV}/power/latest`)));
    assert.equal(latest.child("system/fanRpm").val(), 2200);
    await assertFails(get(ref(piDb, `${DEV}/power/latest`)));
  });

  await check("MEDIA: track buttons and volume, exactly their fields; /media/latest from the agent", async () => {
    for (const [id, value] of [
      ["media-play", { action: "MEDIA", status: "PENDING", media: "PLAY_PAUSE" }],
      ["media-next", { action: "MEDIA", status: "PENDING", media: "NEXT" }],
      ["media-prev", { action: "MEDIA", status: "PENDING", media: "PREVIOUS" }],
      ["media-vol", { action: "MEDIA", status: "PENDING", media: "VOLUME", volume: 35 }],
    ]) {
      await assertSucceeds(set(command(androidDb, id), value));
    }
    for (const [id, value] of [
      ["media-none", { action: "MEDIA", status: "PENDING" }],
      ["media-stop", { action: "MEDIA", status: "PENDING", media: "STOP" }],
      ["media-vol-missing", { action: "MEDIA", status: "PENDING", media: "VOLUME" }],
      ["media-vol-high", { action: "MEDIA", status: "PENDING", media: "VOLUME", volume: 101 }],
      ["media-vol-frac", { action: "MEDIA", status: "PENDING", media: "VOLUME", volume: 12.5 }],
      ["media-next-vol", { action: "MEDIA", status: "PENDING", media: "NEXT", volume: 10 }],
      ["ping-media", { action: "PING", status: "PENDING", media: "NEXT" }],
      ["ping-volume", { action: "PING", status: "PENDING", volume: 10 }],
    ]) {
      await assertFails(set(command(androidDb, id), value));
    }
    await assertSucceeds(update(command(piDb, "media-vol"), { status: "PROCESSING", processingAt: Date.now() }));
    await assertFails(update(command(piDb, "media-vol"), { volume: 90 }));
    await assertFails(update(command(piDb, "media-vol"), { status: "SUCCESS", result: "PONG", processingAt: null }));
    await assertSucceeds(update(command(piDb, "media-vol"), { status: "SUCCESS", result: "MEDIA_DONE", processingAt: null }));
    await assertSucceeds(update(command(piDb, "media-play"), { status: "PROCESSING", processingAt: Date.now() }));
    await assertSucceeds(update(command(piDb, "media-play"), { status: "FAILED", errorCode: "NOT_PERMITTED", processingAt: null }));

    await assertSucceeds(set(ref(piDb, `${DEV}/media/latest`), { volume: 35, muted: false, reportedAt: serverTimestamp() }));
    await assertFails(set(ref(piDb, `${DEV}/media/latest`), { volume: 135, muted: false, reportedAt: serverTimestamp() }));
    await assertFails(set(ref(piDb, `${DEV}/media/latest`), { volume: 35, muted: "no", reportedAt: serverTimestamp() }));
    await assertFails(set(ref(piDb, `${DEV}/media/latest`), { volume: 35, muted: false, reportedAt: serverTimestamp(), track: "x" }));
    await assertFails(set(ref(androidDb, `${DEV}/media/latest`), { volume: 35, muted: false, reportedAt: serverTimestamp() }));
    await assertSucceeds(get(ref(androidDb, `${DEV}/media/latest`)));
    await assertFails(get(ref(otherDb, `${DEV}/media/latest`)));
  });

  await check("RESTART: fresh timestamp, no extra fields, RESTARTING; timed SLEEP may fail NOT_PERMITTED; App Store phase", async () => {
    const restart = (extra = {}) => ({ action: "RESTART", status: "PENDING", createdAt: serverTimestamp(), ...extra });
    await assertSucceeds(set(command(androidDb, "restart-ok"), restart()));
    await assertFails(set(command(androidDb, "restart-no-time"), { action: "RESTART", status: "PENDING" }));
    await assertFails(set(command(androidDb, "restart-old"), restart({ createdAt: Date.now() - 120_000 })));
    await assertFails(set(command(androidDb, "restart-duration"), restart({ durationMinutes: 5 })));
    await assertFails(set(command(androidDb, "restart-text"), restart({ text: "x" })));
    await assertSucceeds(update(command(piDb, "restart-ok"), { status: "PROCESSING", processingAt: Date.now() }));
    await assertFails(update(command(piDb, "restart-ok"), { status: "SUCCESS", result: "POWERING_OFF", processingAt: null }));
    await assertSucceeds(update(command(piDb, "restart-ok"), { status: "SUCCESS", result: "RESTARTING", processingAt: null }));
    await assertSucceeds(set(command(androidDb, "restart-busy"), restart()));
    await assertSucceeds(update(command(piDb, "restart-busy"), { status: "PROCESSING", processingAt: Date.now() }));
    await assertSucceeds(update(command(piDb, "restart-busy"), { status: "FAILED", errorCode: "UPDATE_RUNNING", processingAt: null }));

    await assertSucceeds(set(command(androidDb, "sleep-no-rule"), {
      action: "SLEEP", status: "PENDING", createdAt: serverTimestamp(), durationMinutes: 30,
    }));
    await assertSucceeds(update(command(piDb, "sleep-no-rule"), { status: "PROCESSING", processingAt: Date.now() }));
    await assertSucceeds(update(command(piDb, "sleep-no-rule"), { status: "FAILED", errorCode: "NOT_PERMITTED", processingAt: null }));

    await assertSucceeds(set(ref(piDb, `${DEV}/system/update`), {
      state: "RUNNING", phase: "APP_STORE", percent: 10, done: 0, total: 0, packages: 1, startedAt: Date.now(),
    }));
    await assertSucceeds(set(ref(piDb, `${DEV}/system/update`), {
      state: "FAILED", phase: "FINISHED", percent: 40, error: "UPDATE_EXIT_1", finishedAt: serverTimestamp(),
    }));
  });

  await check("Phone presence: the user writes online/lastSeen, the agent only reads it", async () => {
    await assertSucceeds(set(ref(androidDb, `${DEV}/phone`), { online: true, lastSeen: serverTimestamp() }));
    await assertSucceeds(set(ref(androidDb, `${DEV}/phone`), { online: false, lastSeen: serverTimestamp() }));
    await assertFails(set(ref(androidDb, `${DEV}/phone`), { online: "yes", lastSeen: serverTimestamp() }));
    await assertFails(set(ref(androidDb, `${DEV}/phone`), { online: true }));
    await assertFails(set(ref(androidDb, `${DEV}/phone`), { online: true, lastSeen: Date.now() + 3_600_000 }));
    await assertFails(set(ref(androidDb, `${DEV}/phone`), { online: true, lastSeen: serverTimestamp(), battery: 50 }));
    await assertFails(set(ref(piDb, `${DEV}/phone`), { online: true, lastSeen: serverTimestamp() }));
    await assertFails(set(ref(otherDb, `${DEV}/phone`), { online: true, lastSeen: serverTimestamp() }));
    const seen = await assertSucceeds(get(ref(piDb, `${DEV}/phone`)));
    assert.equal(seen.val().online, false);
    await assertFails(get(ref(otherDb, `${DEV}/phone`)));
  });

  await check("Device names: the user names a MAC, removes it again; the agent only reads them", async () => {
    const alias = (mac) => ref(androidDb, `${DEV}/network/aliases/${mac}`);
    await assertSucceeds(set(alias("AA-BB-CC-DD-EE-FF"), "Living room TV"));
    await assertSucceeds(set(alias("AA-BB-CC-DD-EE-FF"), "TV"));
    await assertFails(set(alias("aa-bb-cc-dd-ee-ff"), "lower case MAC"));
    await assertFails(set(alias("AA:BB:CC:DD:EE:FF"), "colons"));
    await assertFails(set(alias("AA-BB-CC-DD-EE-01"), ""));
    await assertFails(set(alias("AA-BB-CC-DD-EE-02"), "x".repeat(61)));
    await assertFails(set(alias("AA-BB-CC-DD-EE-03"), 42));
    await assertFails(set(ref(piDb, `${DEV}/network/aliases/AA-BB-CC-DD-EE-FF`), "Agent"));
    await assertFails(set(ref(otherDb, `${DEV}/network/aliases/AA-BB-CC-DD-EE-FF`), "Stranger"));
    const names = await assertSucceeds(get(ref(piDb, `${DEV}/network/aliases`)));
    assert.equal(names.val()["AA-BB-CC-DD-EE-FF"], "TV");
    await assertSucceeds(set(alias("AA-BB-CC-DD-EE-FF"), null));
  });

  await check("CAFFEINATE: state ON/OFF only, CAFFEINATE_SET; /power/latest reports it", async () => {
    await assertSucceeds(set(command(androidDb, "caf-on"), { action: "CAFFEINATE", status: "PENDING", state: "ON" }));
    await assertSucceeds(set(command(androidDb, "caf-off"), { action: "CAFFEINATE", status: "PENDING", state: "OFF" }));
    await assertFails(set(command(androidDb, "caf-up"), { action: "CAFFEINATE", status: "PENDING", state: "UP" }));
    await assertFails(set(command(androidDb, "caf-none"), { action: "CAFFEINATE", status: "PENDING" }));
    await assertFails(set(command(androidDb, "caf-text"), { action: "CAFFEINATE", status: "PENDING", state: "ON", text: "x" }));
    await assertFails(set(command(androidDb, "ping-on"), { action: "PING", status: "PENDING", state: "ON" }));
    await assertSucceeds(update(command(piDb, "caf-on"), { status: "PROCESSING", processingAt: Date.now() }));
    await assertFails(update(command(piDb, "caf-on"), { state: "OFF" }));
    await assertFails(update(command(piDb, "caf-on"), { status: "SUCCESS", result: "TAILSCALE_SET", processingAt: null }));
    await assertSucceeds(update(command(piDb, "caf-on"), { status: "SUCCESS", result: "CAFFEINATE_SET", processingAt: null }));
    await assertSucceeds(set(ref(piDb, `${DEV}/power/latest`), { reportedAt: serverTimestamp(), caffeinate: true }));
    await assertFails(set(ref(piDb, `${DEV}/power/latest`), { reportedAt: serverTimestamp(), caffeinate: "on" }));
  });

  await check("Apps: OPEN_APP carries a bundle ID, LIST_APPS nothing; /apps/latest from the agent", async () => {
    const open = (extra = {}) => ({ action: "OPEN_APP", status: "PENDING", app: "com.apple.Safari", ...extra });
    await assertSucceeds(set(command(androidDb, "app-ok"), open()));
    await assertFails(set(command(androidDb, "app-none"), { action: "OPEN_APP", status: "PENDING" }));
    await assertFails(set(command(androidDb, "app-path"), open({ app: "/Applications/Safari.app" })));
    await assertFails(set(command(androidDb, "app-shell"), open({ app: "com.apple.Safari; rm -rf ~" })));
    await assertFails(set(command(androidDb, "app-text"), open({ text: "x" })));
    await assertSucceeds(set(command(androidDb, "list-ok"), { action: "LIST_APPS", status: "PENDING" }));
    await assertFails(set(command(androidDb, "list-app"), { action: "LIST_APPS", status: "PENDING", app: "com.apple.Safari" }));
    await assertFails(set(command(androidDb, "ping-app"), { action: "PING", status: "PENDING", app: "com.apple.Safari" }));
    await assertSucceeds(update(command(piDb, "app-ok"), { status: "PROCESSING", processingAt: Date.now() }));
    await assertFails(update(command(piDb, "app-ok"), { app: "com.apple.Terminal" }));
    await assertSucceeds(update(command(piDb, "app-ok"), { status: "FAILED", errorCode: "BAD_INPUT", processingAt: null }));
    await assertSucceeds(update(command(piDb, "list-ok"), { status: "PROCESSING", processingAt: Date.now() }));
    await assertFails(update(command(piDb, "list-ok"), { status: "FAILED", errorCode: "BAD_INPUT", processingAt: null }));
    await assertSucceeds(update(command(piDb, "list-ok"), { status: "SUCCESS", result: "APPS_LISTED", processingAt: null }));

    const list = ref(piDb, `${DEV}/apps/latest`);
    const item = { bundleId: "com.apple.Safari", name: "Safari", icon: "iVBORw0KGgo=" };
    await assertSucceeds(set(list, { items: { com_apple_Safari: item }, updatedAt: serverTimestamp() }));
    await assertFails(set(list, { items: { "com apple Safari": item }, updatedAt: serverTimestamp() }));
    await assertFails(set(list, { items: { com_apple_Safari: { ...item, path: "/Applications" } }, updatedAt: serverTimestamp() }));
    await assertFails(set(list, { items: { com_apple_Safari: { ...item, icon: "x".repeat(30001) } }, updatedAt: serverTimestamp() }));
    await assertFails(set(ref(androidDb, `${DEV}/apps/latest`), { items: { com_apple_Safari: item }, updatedAt: serverTimestamp() }));
    const read = await assertSucceeds(get(ref(androidDb, `${DEV}/apps/latest/items/com_apple_Safari/name`)));
    assert.equal(read.val(), "Safari");
  });

  await check("Unpairing removes the device and frees the agent; the agent can also reset itself", async () => {
    // Not by strangers.
    await assertFails(set(ref(otherDb, `agents/${PI}`), null));
    await assertFails(set(ref(otherAgentDb, `agents/${PI}`), null));
    await assertFails(set(ref(otherDb, DEV), null));
    await assertFails(set(ref(piDb, DEV), null));
    await assertFails(set(ref(otherDb, INFO), null));
    await assertFails(set(ref(piDb, INFO), null));
    await assertSucceeds(update(ref(androidDb), { [`agents/${PI}`]: null, [INFO]: null, [DEV]: null }));
    assert.equal((await assertSucceeds(get(ref(androidDb, `users/${ANDROID}`)))).exists(), false);
    // The agent is locked out at once, and can be paired again with a new code.
    await assertFails(get(ref(piDb, `${DEV}/commands`)));
    await assertFails(set(ref(piDb, `${DEV}/power/latest`), { reportedAt: serverTimestamp() }));
    await assertSucceeds(set(ref(piDb, `pairing/${PI}`), { code: "HJKM2345", name: "Mac", platform: "macos", createdAt: serverTimestamp() }));
    await assertSucceeds(update(ref(otherDb), {
      [`agents/${PI}`]: { owner: OTHER, code: "HJKM2345", pairedAt: serverTimestamp() },
      [`users/${OTHER}/devices/${PI}`]: { name: "Mac", platform: "macos", pairedAt: serverTimestamp() },
      [`pairing/${PI}`]: null,
    }));
    await assertSucceeds(set(ref(piDb, `users/${OTHER}/data/${PI}/power/latest`), { reportedAt: serverTimestamp() }));
    await assertFails(set(ref(androidDb, `agents/${PI}`), null));
    await assertSucceeds(set(ref(piDb, `agents/${PI}`), null));
    await assertFails(set(ref(piDb, `users/${OTHER}/data/${PI}/power/latest`), { reportedAt: serverTimestamp() }));
  });

  await check("Deleting an account's data: only its owner, in one go", async () => {
    await assertFails(set(ref(androidDb, `users/${OTHER}`), null));
    await assertFails(set(ref(otherAgentDb, `users/${OTHER}`), null));
    await assertFails(set(ref(otherDb, `users/${OTHER}/notes`), "x"));
    await assertSucceeds(set(ref(otherDb, `users/${OTHER}`), null));
    assert.equal((await assertSucceeds(get(ref(otherDb, `users/${OTHER}`)))).exists(), false);
  });
} finally {
  await testEnv.cleanup();
}

process.stdout.write(`PASS: ${checks} Firebase Realtime Database Rules test groups\n`);
