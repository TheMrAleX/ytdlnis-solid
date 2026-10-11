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

Keep a private backup. Never commit the key or passwords. Missing secrets fail CI rather than creating a temporary key. Keep using the same key and workflow file: changing the package/key or resetting the run counter can break upgrades. ABI codes are retained; changing CPU APK within the same build may not be an upgrade. Each new run increments the base version, including rerun attempts (maximum 99 attempts per run). Rerunning an older build produces an older version and does not replace Latest.

## Visual direction

Inspired by classic SnapTube download selectors: warm yellow accents, neutral light surfaces and slate dark surfaces, compact 12dp cards, outlined input controls, and a full-width download action below the media title. Audio/video/command tabs remain accessible. Format rows emphasize quality and file size, with checked backgrounds and borders. History, queue, search, settings, and alternate landscape layouts use the same component language. Theme choices, accessibility, and download features remain available.

Reference: https://www.snaptube.com/ and historical download selector screenshots from https://www.techtudo.com.br/dicas-e-tutoriais/2022/02/como-baixar-video-do-youtube-com-o-snaptube.ghtml

Android update requirements: https://developer.android.com/build/configure-app-module and https://developer.android.com/studio/publish/app-signing
