# Prompt Launcher

A keyboard-first, text-only Android home screen. Type what you want, press Enter, and it happens. Built for phones with a physical keyboard and developed on the Unihertz Titan 2. It runs on Android 9 and later, but other devices are less tested.

> **Status:** Beta 1.2. Expect rough edges. Bug reports are welcome; open an issue or type `bug` in the launcher.

---

## What it does

- **Command bar.** Start typing on the home screen and the prompt opens. Symbols pick the action:

  | Type | Does |
  |---|---|
  | `.` | search and open apps |
  | `@` | message a contact |
  | `/` | call a contact |
  | `:` | add a calendar event |
  | `+` | add a to-do |
  | `"` | write a note |
  | `!` | start a timer |
  | `!!` | set an alarm |
  | `?` | ask the AI assistant (optional, see below) |
  | anything else | search the web, or hand it to Claude |

- **Word commands:** `go <place>` (maps), `email <name>`, `settings`, `calendarsetup`, `weathersetup`, `usagesetup`, `geminisetup`, `todoistsetup`, `bug`.
- **At a glance:** time, date, weather, unread messages, your next calendar event, and an attention card.
- **Productivity dots:** 24 dots, one per hour of the day. An hour turns red once more than 20 minutes of it went to apps you've marked as distracting.
- **Messages, mail, calendar and tasks** without leaving the launcher (Gmail, Google Calendar, Google Tasks or Todoist).
- **Quick toggles:** Do Not Disturb, flashlight and ringer, reachable from the keyboard.
- **Appearance:** themes, accent colours, text size, contrast and reduced motion.

## Privacy

Prompt Launcher has **no server**. Nothing you do is sent to the developer, ever. There are **no analytics, trackers, ads or crash reporters**. The only third-party library is Google's sign-in library, used only if you connect a Google account.

The app only connects to these services:

| Service | When | What it sends |
|---|---|---|
| [Open-Meteo](https://open-meteo.com) | Weather refresh | Approximate location |
| Gmail / Google Calendar / Google Tasks | Only after you sign in | Requests to your own account |
| Todoist | Only if you add your token | Requests to your own tasks |
| Google Gemini | Only when you ask the assistant something | See below |

**AI assistant (optional).** It uses **your own** Gemini API key and sends data straight from your phone to Google. Your question is always sent. Calendar, texts and mail are each **off until you switch them on** in `geminisetup`, and that screen lists exactly what each switch sends. Payloads are kept small: for example, at most your 5 newest emails, trimmed to 150 characters each.

**On the phone.** Your Gemini key and Todoist token are encrypted with the Android Keystore. Everything else lives in the app's private storage, and app backup is turned off.

Don't take our word for it. The whole app is in this repo, and you can watch every connection it makes with a network monitor such as [PCAPdroid](https://github.com/emanuele-f/PCAPdroid).

## Permissions

Onboarding asks for contacts and calendar. Everything else is optional, and a feature only works once its permission is granted.

| Permission | Used for |
|---|---|
| Contacts | Messaging and calling contacts by name |
| Calendar | Showing and adding events |
| Location (approximate) | Weather |
| Usage access | Productivity dots |
| Notification access | Message previews and the attention card |
| SMS / MMS / call log | Reading, replying to and calling from recent messages |

## Build it yourself

**Requirements:** a recent stable Android Studio, and a device or emulator running Android 9 (API 28) or later. Gradle downloads the JDK it needs automatically.

1. Clone the repo and open the project folder in Android Studio.
2. Let Gradle sync, then **Run** on your device.
3. Set Prompt Launcher as your home app when Android asks. Onboarding walks you through the rest.

### Optional: Google sign-in (Gmail, Calendar, Tasks)

Google only allows sign-in from apps registered in a Google Cloud project, so forks need their own:

1. Create a project in the [Google Cloud Console](https://console.cloud.google.com/).
2. Enable the **Gmail API**, **Google Calendar API** and **Google Tasks API**.
3. Configure the **OAuth consent screen**, then add yourself as a test user.
4. Create an **OAuth client ID** of type **Android**, using your package name and your signing certificate's SHA-1. To get the SHA-1, run `./gradlew signingReport` or `keytool -list -v -keystore <your.jks>`.

If you publish a fork, change the `applicationId` in `app/build.gradle.kts` to one of your own.

### Optional: AI assistant

Create a free API key in [Google AI Studio](https://aistudio.google.com/), then type `geminisetup` in the launcher and paste it in.

### Optional: Todoist

Copy your API token from Todoist (**Settings → Integrations → Developer**), then run `todoistsetup`.

## Contributing

Issues and pull requests are welcome. Please keep the launcher keyboard-first and fast. Any change that sends new data off the phone must be opt-in and listed on the relevant setup screen.

## Credits

- Built on ideas and code from [MinkLauncher OpenSource](https://f-droid.org/packages/com.katoaapps.openminilaunch/) by Katoa Apps, licensed under Apache 2.0. See `NOTICE`.
- [Poppins](https://fonts.google.com/specimen/Poppins) font, under the SIL Open Font License 1.1.
- Weather data by [Open-Meteo](https://open-meteo.com).

## License

Licensed under the [Apache License 2.0](LICENSE).
