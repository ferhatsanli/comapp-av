#!/usr/bin/env python3
"""Builds database.rules.json - the multi-user Realtime Database Rules.

Layout (see docs/data-model.md):

  /pairing/{agentUid}                      an unpaired agent's one-time code (agent writes)
  /agents/{agentUid}                       who owns this agent (the user writes it, with the code)
  /users/{uid}/devices/{agentUid}          the short list of paired computers (name, platform, ...)
  /users/{uid}/data/{agentUid}/...         everything else about one paired computer

A user is a signed-in, non-anonymous account. An agent is an anonymous account; it may touch
/users/{uid}/devices|data/{agentUid} only while /agents/{agentUid}/owner is that uid.

The expressions repeat a lot, so they are assembled here instead of being edited by hand:

  python3 tools/build_rules.py          # rewrites database.rules.json
  python3 tools/build_rules.py --check  # fails if the file is out of date (CI)
"""

import json
import sys
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "database.rules.json"

ACTIONS = {
    # action: result when it succeeds
    "PING": "PONG",
    "SPEAK": "SPOKEN",
    "SLEEP": "SLEEPING",
    "SCAN": "SCANNED",
    "SHUTDOWN": "POWERING_OFF",
    "POWER_MODE": "MODE_SET",
    "DOWNLOAD_ADD": "DOWNLOAD_STARTED",
    "SYSTEM_INFO": "REPORTED",
    "TAILSCALE": "TAILSCALE_SET",
    "SYSTEM_UPDATE": "UPDATE_STARTED",
    "MEDIA": "MEDIA_DONE",
}
# Fields the phone sets and the agent must leave alone.
IMMUTABLE = ("action", "text", "createdAt", "durationMinutes", "mode", "language", "state", "source", "url",
             "media", "volume")
PLATFORMS = ("linux", "macos", "windows")
CODE = "/^[A-HJ-NP-Z2-9]{8}$/"   # 8 characters, no 0/O/1/I
PAIRING_TTL_MS = 600000


def any_of(expr, values):
    return "(" + " || ".join("%s === '%s'" % (expr, v) for v in values) + ")"


SIGNED_IN_USER = "auth != null && auth.token.firebase.sign_in_provider !== 'anonymous'"
USER = SIGNED_IN_USER + " && auth.uid === $uid"
OWNED = "root.child('agents').child($deviceId).child('owner').val() === $uid"
# The user, on a device that is paired to them.
USER_DEV = "(" + USER + " && " + OWNED + ")"
# The agent of this device, while it is paired to this user.
AGENT = "(auth != null && auth.uid === $deviceId && " + OWNED + ")"
EITHER = "(" + USER_DEV + " || " + AGENT + ")"


def leaf(validate):
    return {".validate": validate}


def number(extra=""):
    return leaf("newData.isNumber()" + (" && " + extra if extra else ""))


def whole(extra="newData.val() >= 0"):
    return number(extra + " && newData.val() % 1 === 0")


def text(low, high):
    return leaf("newData.isString() && newData.val().length >= %d && newData.val().length <= %d" % (low, high))


def enum(values):
    return leaf("newData.isString() && " + any_of("newData.val()", values))


PAST = number("newData.val() <= now")
NO_OTHER = {"$other": {".validate": False}}


def command_rules():
    a = "newData.child('action').val()"
    d = "data.child('action').val()"
    results = " || ".join("(%s === '%s' && newData.child('result').val() === '%s')" % ("%s", k, v) for k, v in ACTIONS.items())
    timed = "(%s === 'SLEEP' || %s === 'SHUTDOWN')" % (a, a)
    uploaded = ("root.child('users').child($uid).child('data').child($deviceId)"
                ".child('uploads').child($commandId).child('torrent').exists()")
    create = (
        USER_DEV + " && !data.exists() && newData.exists() && newData.child('status').val() === 'PENDING'"
        " && (!%s || (newData.child('createdAt').isNumber() && newData.child('createdAt').val() > now - 60000))"
        " && (%s !== 'DOWNLOAD_ADD' || newData.child('source').val() !== 'TORRENT_FILE' || %s)" % (timed, a, uploaded)
    )
    unchanged = " && ".join("data.child('%s').val() === newData.child('%s').val()" % (f, f) for f in IMMUTABLE)
    s_old, s_new = "data.child('status').val()", "newData.child('status').val()"
    advance = (
        AGENT + " && data.exists() && newData.exists() && " + unchanged + " && ("
        "(%s === 'PENDING' && %s === 'PROCESSING')"
        " || (%s === 'PROCESSING' && data.child('processingAt').val() < now - 120000 && %s === 'PROCESSING')"
        " || (%s === 'PROCESSING' && %s === 'SUCCESS' && (%s))"
        " || (%s === 'PROCESSING' && %s === 'FAILED' && newData.child('errorCode').exists()))"
        % (s_old, s_new, s_old, s_new, s_old, s_new, results % ((d,) * len(ACTIONS)), s_old, s_new)
    )
    # The phone may clear its own history; the agent prunes what is finished.
    remove = ("!newData.exists() && data.exists() && (" + USER_DEV + " || (" + AGENT +
              " && (%s === 'SUCCESS' || %s === 'FAILED')))" % (s_old, s_old))

    def only(action, field):
        return "((%s === '%s' && newData.child('%s').exists()) || (%s !== '%s' && !newData.child('%s').exists()))" % (
            a, action, field, a, action, field)

    shape = (
        "newData.hasChildren(['action', 'status'])"
        " && " + only("SPEAK", "text") +
        " && ((%s && newData.child('createdAt').exists()) || (!%s && !newData.child('createdAt').exists()))" % (timed, timed) +
        " && (%s === 'SLEEP' || !newData.child('durationMinutes').exists())" % a +
        " && (%s === 'SPEAK' || !newData.child('language').exists())" % a +
        " && " + only("TAILSCALE", "state") +
        " && " + only("POWER_MODE", "mode") +
        " && " + only("MEDIA", "media") +
        " && ((%s === 'MEDIA' && newData.child('media').val() === 'VOLUME') === newData.child('volume').exists())" % a +
        " && ((%s === 'DOWNLOAD_ADD' && newData.child('source').exists() && ((newData.child('source').val() === 'TORRENT_FILE'"
        " && !newData.child('url').exists()) || (newData.child('source').val() !== 'TORRENT_FILE' && newData.child('url').exists())))"
        " || (%s !== 'DOWNLOAD_ADD' && !newData.child('source').exists() && !newData.child('url').exists()))" % (a, a) +
        " && ((%s === 'PENDING' && !newData.child('processingAt').exists() && !newData.child('result').exists()"
        " && !newData.child('errorCode').exists())"
        " || (%s === 'PROCESSING' && newData.child('processingAt').isNumber() && newData.child('processingAt').val() >= now - 30000"
        " && !newData.child('result').exists() && !newData.child('errorCode').exists())"
        " || (%s === 'SUCCESS' && !newData.child('processingAt').exists() && (%s) && !newData.child('errorCode').exists())"
        " || (%s === 'FAILED' && !newData.child('processingAt').exists() && newData.child('errorCode').exists()"
        " && !newData.child('result').exists()))" % (s_new, s_new, s_new, results % ((a,) * len(ACTIONS)), s_new)
    )
    pa = "newData.parent().child('action').val()"
    error = (
        "newData.isString() && (newData.val() === 'AGENT_ERROR' || newData.val() === 'NOT_SUPPORTED'"
        " || (%s && %s === 'DOWNLOAD_ADD')"
        " || (newData.val() === 'NOT_PERMITTED' && (%s === 'TAILSCALE' || %s === 'SYSTEM_UPDATE' || %s === 'MEDIA'))"
        " || (newData.val() === 'UPDATE_RUNNING' && (%s === 'SYSTEM_UPDATE' || %s === 'SLEEP' || %s === 'SHUTDOWN'))"
        " || (newData.val() === 'SLEEP_CYCLE_ACTIVE' && %s === 'SLEEP'))"
        % (any_of("newData.val()", ("NOT_A_FILE", "NO_SPACE", "BAD_INPUT", "ENGINE_DOWN")), pa, pa, pa, pa, pa, pa, pa, pa)
    )
    return {
        # The user reads everything under /users/{uid} anyway (it is all theirs); the agent
        # reads all of its commands, and prunes finished ones to keep the list short.
        ".read": AGENT,
        "$commandId": {
            ".read": EITHER,
            ".write": "(" + create + ") || (" + advance + ") || (" + remove + ")",
            ".validate": shape,
            "action": enum(ACTIONS),
            "durationMinutes": whole("newData.val() >= 1 && newData.val() <= 1440"),
            "mode": enum(("PERFORMANCE", "POWERSAVE")),
            "createdAt": PAST,
            "state": enum(("UP", "DOWN")),
            "language": enum(("tr", "en")),
            "text": text(1, 300),
            "status": enum(("PENDING", "PROCESSING", "SUCCESS", "FAILED")),
            "processingAt": number("newData.val() >= now - 30000"),
            "result": enum(ACTIONS.values()),
            "errorCode": leaf(error),
            "source": enum(("MAGNET", "URL", "TORRENT_FILE")),
            "media": enum(("PREVIOUS", "PLAY_PAUSE", "NEXT", "VOLUME")),
            "volume": whole("newData.val() >= 0 && newData.val() <= 100"),
            "url": leaf(
                "newData.isString() && newData.val().length <= 4096 && ((newData.parent().child('source').val() === 'MAGNET'"
                " && newData.val().matches(/^magnet:[?][^ ]+$/)) || (newData.parent().child('source').val() === 'URL'"
                " && newData.val().matches(/^https?:[/][/][^ ]+$/)))"),
            **NO_OTHER,
        },
    }


def agent_report(body, read=USER_DEV):
    """A node the agent writes and the phone reads."""
    return {".read": read, ".write": AGENT, **body}


def info_rules():
    """/users/{uid}/devices/{deviceId}: the entry in the user's list of computers. Kept apart
    from the data so the app can list computers without downloading their reports."""
    new_root = "newData.parent().parent().parent().parent()"
    return {
        ".read": EITHER,
        # The user creates it while pairing (in the same update that claims the agent), may
        # rename the device, and removes it when unpairing; the agent keeps its own facts fresh.
        ".write": "(" + USER + " && (!newData.exists() || " + new_root +
                  ".child('agents').child($deviceId).child('owner').val() === $uid)) || (" + AGENT + " && newData.exists())",
        ".validate": "newData.hasChildren(['name', 'platform', 'pairedAt'])",
        "name": text(1, 40),
        "platform": enum(PLATFORMS),
        "pairedAt": PAST,
        "hostname": text(1, 80),
        "osVersion": text(1, 80),
        "agentVersion": leaf("newData.isString() && newData.val().matches(/^[0-9]{1,4}(\\.[0-9]{1,4}){1,2}$/)"),
        # What this computer can do; the phone hides the rest.
        "capabilities": {
            "$name": leaf("$name.matches(/^[a-zA-Z]{1,30}$/) && newData.isBoolean()"),
        },
        **NO_OTHER,
    }


def data_rules():
    intervals = "(newData.val() === 10 || newData.val() === 20 || newData.val() === 30 || newData.val() === 60)"
    return {
        # Unpairing: the user removes the whole node (with /agents/{id} and the list entry).
        ".write": USER + " && !newData.exists()",
        "commands": command_rules(),
        "uploads": {
            "$commandId": {
                ".read": AGENT,
                ".write": "(" + USER_DEV + " && !data.exists() && newData.exists()) || (" + EITHER + " && data.exists() && !newData.exists())",
                ".validate": "newData.hasChildren(['torrent'])",
                "torrent": leaf("newData.isString() && newData.val().length > 0 && newData.val().length <= 2800000"),
                **NO_OTHER,
            },
        },
        "settings": {
            "sleepIntervalMinutes": {".read": EITHER, ".write": USER_DEV, ".validate": "newData.isNumber() && " + intervals},
            "downloadLimitMbps": {
                ".read": EITHER, ".write": USER_DEV,
                ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 10000 && newData.val() % 1 === 0",
            },
        },
        "network": {
            "latest": agent_report({
                ".validate": "newData.hasChildren(['scannedAt', 'devices'])",
                "scannedAt": PAST,
                "devices": {
                    "$mac": {
                        ".validate": "$mac.matches(/^([0-9A-F]{2}-){5}[0-9A-F]{2}$/) && newData.hasChildren(['ip', 'name'])",
                        "ip": leaf("newData.isString() && newData.val().matches(/^(\\d{1,3}\\.){3}\\d{1,3}$/)"),
                        "name": text(1, 60),
                        **NO_OTHER,
                    },
                },
                **NO_OTHER,
            }),
        },
        "power": {
            "latest": agent_report({
                ".validate": "newData.hasChildren(['reportedAt'])",
                "reportedAt": PAST,
                "powerMode": enum(("PERFORMANCE", "POWERSAVE", "UNKNOWN")),
                "cpu": {
                    "tempC": number("newData.val() >= -40 && newData.val() <= 150"),
                    "usagePercent": number("newData.val() >= 0 && newData.val() <= 100"),
                    "packageWatts": number("newData.val() >= 0 && newData.val() <= 500"),
                    "coreFreqMhz": {
                        "$core": leaf("$core.matches(/^cpu[0-9]+$/) && newData.isNumber() && newData.val() >= 0 && newData.val() <= 10000"),
                    },
                    **NO_OTHER,
                },
                "battery": {"percent": number("newData.val() >= 0 && newData.val() <= 100"), **NO_OTHER},
                "system": {
                    "fanRpm": number("newData.val() >= 0 && newData.val() <= 20000"),
                    "ecTempC": number("newData.val() >= -40 && newData.val() <= 150"),
                    **NO_OTHER,
                },
                "desktop": enum(("ON", "OFF")),
                "tailscale": enum(("RUNNING", "STOPPED", "STARTING", "NEEDS_LOGIN", "DAEMON_OFF", "NOT_INSTALLED", "UNKNOWN")),
                **NO_OTHER,
            }),
            "cycle": agent_report({
                ".validate": "newData.hasChildren(['state', 'intervalMinutes', 'updatedAt'])",
                "state": enum(("OFF", "AWAKE", "SLEEPING")),
                "intervalMinutes": leaf("newData.isNumber() && " + intervals),
                "updatedAt": PAST,
                "nextWakeAt": number("newData.parent().child('state').val() === 'SLEEPING' && newData.val() > now - 86400000"
                                     " && newData.val() < now + 86400000"),
                "hold": leaf("newData.isString() && newData.parent().child('state').val() === 'AWAKE' && " + any_of(
                    "newData.val()", ("NETWORK", "DOWNLOAD", "UPDATE", "SESSION", "ALARM_FAILED", "SUSPEND_FAILED"))),
                "detail": leaf("newData.isString() && newData.val().length <= 160 && newData.parent().child('state').val() === 'AWAKE'"
                               " && newData.parent().child('hold').exists()"),
                **NO_OTHER,
            }),
            "history": agent_report({
                "$t": {
                    ".validate": "$t.matches(/^[0-9]{13}$/) && (newData.hasChild('cpu') || newData.hasChild('w'))",
                    "cpu": number("newData.val() >= 0 && newData.val() <= 100"),
                    "w": number("newData.val() >= 0 && newData.val() <= 500"),
                    **NO_OTHER,
                },
            }, read=EITHER),
        },
        "downloads": {
            "latest": agent_report({
                ".validate": "newData.hasChildren(['reportedAt', 'appliedLimitMbps'])",
                "reportedAt": PAST,
                "appliedLimitMbps": whole("newData.val() >= 0 && newData.val() <= 10000"),
                "items": {
                    "$id": {
                        ".validate": "$id.matches(/^[A-Za-z0-9_-]{1,40}$/) && newData.hasChildren(['name', 'kind', 'state',"
                                     " 'totalBytes', 'doneBytes', 'downBps', 'upBps'])",
                        "name": text(1, 200),
                        "kind": enum(("TORRENT", "URL")),
                        "state": enum(("QUEUED", "DOWNLOADING", "SEEDING", "COMPLETE", "FAILED", "PAUSED")),
                        "totalBytes": number("newData.val() >= 0"),
                        "doneBytes": number("newData.val() >= 0"),
                        "downBps": number("newData.val() >= 0"),
                        "upBps": number("newData.val() >= 0"),
                        "etaSec": number("newData.val() >= 0"),
                        "error": text(1, 40),
                        **NO_OTHER,
                    },
                },
                **NO_OTHER,
            }),
        },
        "media": {
            "latest": agent_report({
                ".validate": "newData.hasChildren(['volume', 'muted', 'reportedAt'])",
                "volume": whole("newData.val() >= 0 && newData.val() <= 100"),
                "muted": leaf("newData.isBoolean()"),
                "reportedAt": PAST,
                **NO_OTHER,
            }),
        },
        "system": {
            "update": agent_report({
                ".validate": "newData.hasChildren(['state', 'phase', 'percent'])",
                "state": enum(("RUNNING", "DONE", "FAILED")),
                "phase": enum(("REFRESHING", "DOWNLOADING", "INSTALLING", "VERIFYING", "FINISHED", "UP_TO_DATE")),
                "percent": whole("newData.val() >= 0 && newData.val() <= 100"),
                "done": whole(),
                "total": whole(),
                "packages": whole(),
                "startedAt": PAST,
                "finishedAt": PAST,
                "error": leaf("newData.isString() && newData.val().length <= 40"),
                "rebootRecommended": leaf("newData.isBoolean()"),
                **NO_OTHER,
            }),
        },
        **NO_OTHER,
    }


def build():
    self_agent = "auth != null && auth.uid === $agentUid"
    new_root = "newData.parent().parent()"
    pairing = "root.child('pairing').child($agentUid)"
    return {
        "rules": {
            ".read": False,
            ".write": False,
            "pairing": {
                ".indexOn": ["code"],
                # A signed-in user looks one code up; nobody can list the codes.
                ".read": SIGNED_IN_USER + " && query.orderByChild === 'code' && query.equalTo !== null && query.limitToFirst === 1",
                "$agentUid": {
                    ".read": self_agent,
                    # The agent publishes and withdraws its own code; the user who claims the
                    # agent removes the code in the same update.
                    ".write": "(" + self_agent + ") || (" + SIGNED_IN_USER + " && !newData.exists() && " + new_root +
                              ".child('agents').child($agentUid).child('owner').val() === auth.uid)",
                    ".validate": "newData.hasChildren(['code', 'name', 'platform', 'createdAt'])",
                    "code": leaf("newData.isString() && newData.val().matches(%s)" % CODE),
                    "name": text(1, 40),
                    "platform": enum(PLATFORMS),
                    "createdAt": leaf("newData.isNumber() && newData.val() === now"),
                    **NO_OTHER,
                },
            },
            "agents": {
                "$agentUid": {
                    ".read": "auth != null && (auth.uid === $agentUid || data.child('owner').val() === auth.uid)",
                    ".write": (
                        # Claim: a signed-in user who knows the agent's fresh code becomes its owner.
                        "(" + SIGNED_IN_USER + " && !data.exists() && newData.child('owner').val() === auth.uid && "
                        + pairing + ".child('code').val() === newData.child('code').val() && "
                        + pairing + ".child('createdAt').val() > now - %d)" % PAIRING_TTL_MS +
                        # Unpair: the owner, or the agent itself (uninstall / reset).
                        " || (auth != null && data.exists() && !newData.exists() && (data.child('owner').val() === auth.uid"
                        " || auth.uid === $agentUid))"
                    ),
                    ".validate": "newData.hasChildren(['owner', 'code', 'pairedAt'])",
                    "owner": leaf("newData.isString() && newData.val().length >= 1 && newData.val().length <= 128"),
                    "code": leaf("newData.isString() && newData.val().matches(%s)" % CODE),
                    "pairedAt": leaf("newData.isNumber() && newData.val() === now"),
                    **NO_OTHER,
                },
            },
            "users": {
                "$uid": {
                    ".read": USER,
                    # Deleting the account's data (before the account itself).
                    ".write": USER + " && !newData.exists()",
                    "devices": {"$deviceId": info_rules()},
                    "data": {"$deviceId": data_rules()},
                    **NO_OTHER,
                },
            },
        }
    }


def main():
    rendered = json.dumps(build(), indent=2) + "\n"
    if "--check" in sys.argv:
        if OUT.read_text() != rendered:
            sys.exit("database.rules.json is out of date: run python3 tools/build_rules.py")
        return
    OUT.write_text(rendered)
    print("wrote %s (%d bytes)" % (OUT.name, len(rendered)))


if __name__ == "__main__":
    main()
