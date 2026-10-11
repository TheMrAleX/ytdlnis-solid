# YTDLnis Solid

Independent Android app based on YTDLnis, retaining upstream attribution and GPL-3.0 licensing.

## Installation and updates

The fixed application ID is `com.themralex.ytdlnis.solid`. It can coexist with `com.deniscerri.ytdl`. Kotlin's namespace remains unchanged so existing classes and activity aliases continue to work. Android shortcuts point to Solid.

Every successful Actions run publishes a uniquely tagged release with all three flavors and five CPU variants, plus SHA256 checksums. Main builds become stable releases; other branches are prereleases. Old releases and APKs are retained. The in-app updater reads this fork's stable releases, not upstream. Prefer the github flavor for in-app installation.

One permanent keystore is required before CI can publish. Configure repository Actions secrets:

- `BASE_64_SIGNING_KEY`: base64-encoded JKS keystore.
- `LOCAL_PROPERTIES`: the following exact properties, using that key's actual passwords and alias:

```properties
signingConfig.storeFile=release_keystore.jks
signingConfig.storePassword=YOUR_STORE_PASSWORD
signingConfig.keyAlias=YOUR_KEY_ALIAS
signingConfig.keyPassword=YOUR_KEY_PASSWORD
```

The validation job compiles resources and Kotlin and runs unit tests/lint without creating an installable APK, even before signing is configured. Publication requires all validation and signed build jobs to pass.

If this is your first Solid key, create it on your own computer with JDK 17 and GitHub CLI installed:

```bash
bash scripts/setup_solid_signing.sh
```

The script asks for a password locally, saves a private backup in the git-ignored `solid-signing-private` directory, and uploads the two encrypted secrets to this fork. It refuses to overwrite an existing signing configuration. Do not send the password or key in chat.

Keep a private backup. Never commit the key or passwords. Missing secrets fail CI rather than creating a temporary key. Keep using the same key and workflow file: changing the package/key or resetting the run counter can break upgrades. ABI codes are retained; changing CPU APK within the same build may not be an upgrade. Each new run increments the base version, including rerun attempts (maximum 99 attempts per run). Rerunning an older build produces an older version and does not replace Latest.

## Visual direction

Solid 2.0 replaces the old screen composition: a branded home dashboard with working search/library/queue shortcuts, floating navigation dock, media results with separate thumbnail, title and action areas, readable library rows, transfer cards with a dedicated progress track, grouped tools, neutral metadata tiles and a download action after quality selection. Inputs, tabs, preferences, empty states, onboarding and alternate layouts share new typography, spacing and surfaces. Teal and charcoal are the default; amber is limited to the main home action and the new launcher mark. The new home-screen widget opens the app, library or queue without background polling.

Existing installations switch once to the Solid identity, then respect later accent choices. The application ID, signing key and monotonic version numbering are unchanged. The emulator UI check navigates real screens and captures light/dark previews; the native component gallery uses fixture text only in instrumentation tests. Releases require this check, lint, unit tests and all signed builds to pass.

Reference: https://www.snaptube.com/ and historical download selector screenshots from https://www.techtudo.com.br/dicas-e-tutoriais/2022/02/como-baixar-video-do-youtube-com-o-snaptube.ghtml

Android update requirements: https://developer.android.com/build/configure-app-module and https://developer.android.com/studio/publish/app-signing
