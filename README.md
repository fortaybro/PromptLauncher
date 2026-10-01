# Prompt Launcher

A keyboard-first, text-only Android home screen. Type what you want, press Enter, and it happens. Built for phones with a physical keyboard and developed on the Unihertz Titan 2. It runs on Android 9 and later, but other devices are less tested.

> **Status:** Beta 1.3.1. Expect rough edges. Bug reports are welcome; open an issue or type `bug` in the launcher.

## Download

**[⬇ Download the latest APK](https://github.com/fortaybro/PromptLauncher/releases/latest)**. Open the release and tap the `.apk` file under **Assets**.

To install, allow your browser or file manager to "install unknown apps" when Android asks, then open the APK. After installing, set Prompt Launcher as your home app.

Updating from an earlier version keeps your settings. Onboarding doesn't run again, so set anything new (like your email apps) in **Settings**.

---

## What it does

### The prompt

Start typing on the home screen and the prompt opens. A symbol at the start picks the action:

| Type | Does |
|---|---|
| `@name message` | text someone |
| `@@` | text a group |
| `/name` | call someone |
| `:` | add a calendar event |
| `+` | add a to-do |
| `"` | write a note |
| `!` | start a timer |
| `!!` | set an alarm |
| `?` | ask the AI assistant (optional, see below) |
| `.` | open the full app list (press Enter) |

**Plain text** (no symbol) goes to whichever chip is selected above the prompt:

- **chrome:** searches the web
- **claude:** sends the text to the Claude app
- **apps:** finds and opens an app. Type `todo` and press Enter to open Todoist. The app list takes the place of the command legend, so nothing on screen moves.

Prompt remembers your choice. Pick which chips appear under **Settings → home screen → search picker**.

**Tasks:** while typing `+`, chips above the prompt pick where the to-do goes: **google tasks** or **todoist**. Switching keeps both accounts signed in, so your Todoist token is never asked for twice. Pick which chips appear under **Settings → home screen → task picker**.

**Word commands:** `go <place>` (maps), `email <name>`, `settings`, `calendarsetup`, `weathersetup`, `usagesetup`, `geminisetup`, `taskssetup` (Google Tasks or Todoist), `bug`.

### Texting

Prompt works alongside your messaging app (Google Messages, Samsung Messages, etc.) rather than replacing it:

- **`@name message` + Enter** sends the text and opens that conversation in your messaging app.
- **Tap a name** in the suggestions to lock that exact contact and number in. The `>` turns into a red `@`, the line above reads "message → name", and you type only the message. Backspace on an empty prompt, or Back, unlocks it.
- **`@@`** starts a group. Tap people to add them. Press Enter to open the group in your messaging app, or type a message first to send it to everyone.
- **In the hub,** tapping a conversation or "text" on a call opens it in your messaging app.

Texts sent from Prompt go out as regular SMS/MMS, not RCS chat.

### Email

Email works the same way as texting: Prompt shows it, and your email app opens it.

- **New mail from your email apps** (Gmail, Spark, Samsung Email, Outlook, Yahoo, Proton and others) appears on the home screen card and in the hub's email tab.
- **Tapping an email** opens that exact email in its app.
- **Mail** (the home button or the `mail` command) opens your main email app.
- **`email <name>`** writes a new email, either in Prompt (Gmail) or in the app you choose.
- **Replying from the notification** works through ⚡ quick reply, for apps whose notifications have a Reply button.

Pick your email apps during onboarding, or later in **Settings → noti → EMAIL APPS**. The first one you pick is your main app. Choose where new emails are written under **COMPOSE EMAIL IN**.

### Everything else

- **At a glance:** time, date, weather, unread texts and new mail, and your next calendar event. Weather shows °F or °C (**Settings → home screen → weather → celsius**).
- **Productivity dots:** 24 dots, one for each hour of the day. An hour turns red once more than 20 minutes of it went to apps you've marked as distracting.
- **Calendar and tasks:** Google Calendar, plus Google Tasks or Todoist.
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

**Notifications and texts stay on the phone.** Prompt reads notifications so it can show new texts and email on the home screen and in the hub. When you send a text from the prompt, your phone sends it directly. None of this leaves your device through Prompt.

**Stored on the phone.** Your Gemini key and Todoist token are encrypted with the Android Keystore. Everything else lives in the app's private storage, and app backup is turned off.

Don't take our word for it. The whole app is in this repo, and you can watch every connection it makes with a network monitor such as [PCAPdroid](https://github.com/emanuele-f/PCAPdroid).

## Permissions

Onboarding asks for contacts and calendar. Everything else is optional, and a feature only works once its permission is granted.

| Permission | Used for |
|---|---|
| Contacts | Texting, calling and emailing people by name |
| Calendar | Showing and adding events |
| Location (approximate) | Weather |
| Usage access | Productivity dots |
| Notification access | New texts and email on the home screen and in the hub, and ⚡ quick replies |
| Send SMS | Sending texts and group texts you type in the prompt |
| SMS / MMS / call log | Recent conversations and calls in the hub |

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

Copy your API token from Todoist (**Settings → Integrations → Developer**), then run `taskssetup` and paste it in.

### Optional: Google Tasks

Skipped it during onboarding? Run `taskssetup` and tap **connect google tasks**.

## Contributing

Issues and pull requests are welcome. Please keep the launcher keyboard-first and fast. Any change that sends new data off the phone must be opt-in and listed on the relevant setup screen.

## Credits

- Built on ideas and code from [MinkLauncher OpenSource](https://f-droid.org/packages/com.katoaapps.openminilaunch/) by Katoa Apps, licensed under Apache 2.0. See `NOTICE`.
- [Poppins](https://fonts.google.com/specimen/Poppins) font, under the SIL Open Font License 1.1.
- Weather data by [Open-Meteo](https://open-meteo.com).

## License

Licensed under the [Apache License 2.0](LICENSE).
