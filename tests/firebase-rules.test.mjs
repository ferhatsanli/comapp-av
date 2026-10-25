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
} finally {
  await testEnv.cleanup();
}

process.stdout.write(`PASS: ${checks} Firebase Realtime Database Rules test groups\n`);
