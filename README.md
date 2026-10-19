# Orbit

A personal Android app for staying close to your friends. It pulls in your phone
contacts, has you "file" each one (how you know them, can I hang out with them,
can I call them), then runs a **call rotation**: a daily nudge to call one friend,
cycling through everyone you marked callable — then rinse and repeat.

Everything stays on your phone. Nothing is uploaded anywhere.

## What it does

- **File your people** — contacts show up one at a time. For each: how you know
  them, a relationship tag (friend / family / work / …), notes, and two switches:
  *Can I hang out with them?* and *Can I call them?* Hit **Done — next** and the
  next contact appears. **Skip** sends someone to the back of the queue; **Archive**
  is for businesses and numbers that aren't really people.
- **New-contact watch** — a background job re-syncs contacts every 6 hours. When
  you save a new number, you get a notification to file that person.
- **Call rotation** — everyone marked "can call" goes into a round. The dashboard
  always shows who's *next up* (whoever it's been longest since you caught up with),
  with one-tap **Call** and **Caught up** buttons. When you've called everyone, the
  round completes and you start the next one.
- **Daily nudge** — at a time you pick in Settings, a notification suggests one
  friend from the rotation, with a "Call now" action right on the notification.
- **Friends list** — searchable, filterable by *can call* / *can hang out*, shows
  when you last caught up with each person.
- **Dashboard** — filing progress, rotation progress for the current round,
  stat tiles (callable friends, hangout crew, total catch-ups), and recent history.
- **Instagram import** — merges in accounts you follow (see below).

## Building & installing

You need [Android Studio](https://developer.android.com/studio) (free; bundles its
own JDK and Android SDK).

1. Open Android Studio → **Open** → select this folder (`ContactsFiltering`).
2. Let Gradle sync finish (first time downloads dependencies).
3. On your phone: Settings → About phone → tap **Build number** 7× to enable
   Developer options, then enable **USB debugging** (Settings → Developer options).
4. Plug the phone in, accept the debugging prompt, pick your device in the
   Studio toolbar, and press **Run ▶**.

Or from the command line with an Android SDK installed:

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Instagram import

Instagram shut down API access to personal follow lists, so the app imports from
Instagram's official data export instead:

1. Instagram app → your profile → menu → **Accounts Center**
2. **Your information and permissions** → **Download your information**
3. Request an export — select **Followers and following**, format **JSON**
4. When the export arrives (usually minutes), unzip it and transfer it to
   your phone (or do the whole thing on the phone)
5. In Orbit: **Settings → Import following.json** and pick
   `connections/followers_and_following/following.json`

Accounts whose username matches a contact's name (e.g. `john_doe99` ↔ "John Doe")
get linked to that contact. Everyone else is added as a new person in the filing
queue, and if you later save their number as a contact, the two records merge
automatically on the next sync.

## iOS version

A native SwiftUI port lives in `ios/` (SwiftData storage, same features and data
model). It was written without Xcode on this machine, so unlike the Android app it
has **not** been compile-verified — expect possibly a few small fixes on first build.

To build it:

1. Install [Xcode](https://apps.apple.com/app/xcode/id497799835) from the Mac App
   Store (~12 GB) and launch it once to install the iOS platform.
2. Open `ios/Orbit.xcodeproj`, select the Orbit target → Signing & Capabilities →
   pick your Team (your Apple ID works).
3. Plug in the iPhone, trust the Mac, select it as the run destination, press **Run ▶**.
4. On the phone: Settings → General → VPN & Device Management → trust your
   developer certificate.

iOS-specific differences:

- With a **free** Apple ID the app expires after 7 days — re-run from Xcode to
  renew. A paid Apple Developer account ($99/yr) extends that to a year.
- iOS doesn't allow guaranteed background work, so the every-6-hours contact sync
  is opportunistic (`BGAppRefreshTask`) — new-contact detection mostly happens when
  you open the app.
- iOS can't run app code at notification delivery time, so the daily nudge schedules
  a week ahead: the next nudge names the actual next-up friend, later days say
  "open Orbit to see who's next," and the week is re-planned with a named friend
  every time the app runs.
- The `.xcodeproj` is generated from `ios/project.yml` with
  [XcodeGen](https://github.com/yonaskolb/XcodeGen) (installed via Homebrew);
  re-run `xcodegen generate` in `ios/` if you add/remove Swift files.

## Notes

- The daily nudge uses WorkManager, so aggressive battery savers can delay it by
  a few minutes. If nudges don't arrive, exempt Orbit from battery optimization
  (Settings → Apps → Orbit → Battery → Unrestricted).
- "Call" opens the dialer with the number pre-filled — the app never places calls
  by itself and never needs call permissions.
- Archived people never reappear; they're just hidden (the contact itself is untouched —
  the app never writes to your address book).
