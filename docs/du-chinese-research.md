# Du Chinese 3.6.1 research

Static inspection on 2026-11-02 found a local subscription preference and lesson-opening checks. The experimental patch changes the subscription getter and one confirmed next-lesson navigation check. A separate patch redirects native Google service references to MicroG. Offline emulator startup passed; server subscriptions, protected downloads and authenticated device behavior remain unverified.

## Artifact

| Item | Value |
| --- | --- |
| Original | Workspace `Du Chinese.apks`; preserved |
| APKS SHA-256 | `7da2e66090857375d4c5df7a8d440c311c5192ab972d2d9775dc4db1ed7e9dec` |
| Extracted base APK SHA-256 | `cb326c447ca0272a0a23fbe51cde3bb4b8953d9293a937629f2a49d798a4ea45` |
| Package | `org.sinamon.duchinese` |
| Version | `3.6.1`, version code `4178` |
| SDK | Minimum 21; target 36 |
| Splits | ABI `arm64_v8a`; locales `en`, `ja`, `zh`; density `xxxhdpi` |

Analysis files are under `/private/tmp/du-chinese-research`: `base.apk`, `artifact-identity.json`, `AndroidManifest.xml`, `dex-inventory.json`, `google-strings.txt` and `android-evidence.json`. REA used JADX headless 0.7.1 with JDK 21.0.12.1. APK signature verification passed during output checks; publisher identity was not independently verified. Package evidence: `ev_9e54222cb8215647c74ff8de22c1bbf09dbaed2a65b3c513eba52313cb2d929d`.

## Observed local checks

- Raw `Lorg/sinamon/duchinese/models/o;->M(Landroid/content/Context;)Z` reads the default shared preference `isSubscribed`, defaulting to false. REA found 30 direct using methods. Evidence: `ev_cd5e2d5af40361f08fe62a190800f65b30bca91fd9b58a58410276204156ea66`, `ev_9761bde65b5f3d0931aff3aa28bb139e7f8709336d5cd3e4f814edf89fa418ca`.
- `Zb.E.C(ListDocument, navController, Context, onPremiumRequired)` calls the premium-required callback when `lesson.isLocked()` is true and `o.M(context)` is false; otherwise it constructs lesson navigation state. Evidence: `ev_36254790e5cb003c9233bfeed362d391862871cdc15aaf0e341c7abf46a717d4`.
- Raw `Lec/J;->f(Landroid/content/Context;Lorg/sinamon/duchinese/services/media/PlaybackSpeed;)Lec/K;` refuses next-lesson navigation when the next lesson is absent or locked. It subsequently saves the reading-list position and returns a navigation result. Its `ListDocument.isLocked()` invoke is at code-unit offset 15 in `classes3.dex`. Evidence: `ev_0f1c515a7660ef3c18afb1d29fd8d7cb165403ced041fed997d66905715b1fc2`, `ev_3f67284a18d35f367df6e1446139a15f4755c8f15629b8bafaa5027a6778d1dd`.
- `ListDocument.isLocked()Z` reads its boolean backing field. The getter has 15 direct using methods, including UI checks, copies and storage. `Db.n.c` writes its result into the `is_locked` database column. The patch leaves this getter intact. Evidence: `ev_1ba1e8b6d089a2a7fc5e4a02cacb28a542406e3d3a62fd87331678883c75e981`, `ev_f7c0f3099de5207ade2ceeb82b80d16b4938148f463d2bd71ee3cdb0ae0a5a6b`.

`Enable local Premium controls` defaults to false and declares only inspected version 3.6.1 as experimental. It returns true from `o.M` and replaces the immediate boolean result of the single `ListDocument.isLocked()` call in `ec.J.f` with false. Exact class, method and parameter fingerprints, the `isSubscribed`/`context` strings, and invoke/result opcode checks make it fail when the expected code changes. The explicit next-lesson override covers one caller; the other 14 raw lesson-lock consumers remain unchanged.

**Inference:** these edits should enable the local controls that use the shared subscription getter and allow the confirmed next-lesson navigation path to proceed. They do not establish that content exists locally or that a server will return it.

## Billing and server state

- Play Billing is declared in the manifest. `zb.d.g` stores the real subscription SKU, order ID and token before calling `o.j0(context, true)`. This code is unchanged. Evidence: `ev_a283785ccefd33d8b459a90893555e6bbd5ca644b51b07caa0795ea2d1589ca9`.
- The home response handler `org.sinamon.duchinese.ui.fragments.home.y.h3` parses `MyPageResponse`, caches it, and writes `myPageResponse.isSubscribed()` through `o.j0`. Thus the preference also reflects received server state. Evidence: `ev_f0f5b2a70fc8181e989a938f65dd7809003d04dbc1219c3e18a6c0825f6e7796`.
- `SubscriptionActivity.R0` requests `purchases/status`, passing an existing subscription token as `t` and, when logged in, user UUID/token. The response contains account/device subscription booleans and Play subscription state/expiry. The observed account-subscription getter is used by subscription-screen presentation; it is unchanged. Evidence: `ev_c1d7aa181ea26a716460ad9bb99b9044430e18bce802d187a1efa28bd16a9113`, `ev_627d1961bee6c1d41a0103a2f945c82d3d2c34fa1e612da8a424e8f6ee5c0858`, `ev_f6ff8415523172f0f2eebcc68832b9b83e7b13070240934081340a918b7b7627`.

**Unknown:** enforcement on lesson, audio and vocabulary endpoints, receipt validation results and account-level entitlement. No receipts were fabricated and no live endpoint was contacted. The patch alters neither stored billing data nor these response handlers.

## Google services and MicroG applicability

The native login activity inspected builds an email/password form. The retained `GoogleSignInAccount` references belong to obfuscated Google library classes; they do not demonstrate an app login flow. Raw base-DEX class inventory contains none of the exact classes `GoogleSignInClient`, `GoogleSignInOptions`, `GetGoogleIdOption` or `androidx.credentials.CredentialOption`. An obfuscated or dynamic Google login path remains unproven. Evidence: `ev_69a4d068ec0938aac296556377964e9d58abe3c71a3e744aa66044cd5095b5ef`, `ev_dc9decd56b53fecd4f31708dddf020846faa1a7e9312916f4a15def50aed28f1`.

FCM is active in shipped native code. `DuFcmMessagingService.r(String)` handles a new token and forwards it to account storage; `o.w0` constructs a `user_devices` request containing the account UUID/token and `gcm_token`. The manifest declares an exported Firebase receiver protected by `com.google.android.c2dm.permission.SEND`, the C2DM receive action, and non-exported services using the internal `com.google.firebase.MESSAGING_EVENT` action. Evidence: `ev_3470db934ae6a4bcef587a27ab8c4115450eafa15e3f7542b31de54ab87db06b`, `ev_338e8b1a426ce46b32c012bc75998d46a0f02978e2c6648d7dd34d3ebad20b4b`, package evidence above.

The availability checks are obfuscated:

| Raw method | Observed behavior |
| --- | --- |
| `LY5/n;->f(Landroid/content/Context;I)I` | Google services manifest, package, signature, version and enabled checks; codes 0/1/2/3/9 |
| `LY5/k;->h(Landroid/content/Context;I)I` | Calls `Y5.n.f`; returns updating code 18 or the underlying result |

These correspond to the `GooglePlayServicesUtilLight` and `GoogleApiAvailabilityLight` patterns, so a shared MicroG helper needs these exact descriptors rather than the unobfuscated names. JADX reports duplicated-block warnings in `Y5.n.f`; its reconstructed branches are unsuitable for proving precise control flow. Evidence: `ev_cc31a46fe80dea10eba3cf657a796842af72206ced6463f2b46271d5bcb2343f`, `ev_90b46a555ddae29ea08b217be4d61287191fa503f1ed265da5f96f51a0a75275`.

The base DEX contains these external service-action constants:

| Action | Routing finding |
| --- | --- |
| `com.google.android.gms.common.telemetry.service.START` | MicroG-RE vendor alias previously confirmed |
| `com.google.android.gms.signin.service.START` | MicroG-RE vendor alias previously confirmed; app sign-in use unproven |
| `com.google.android.gms.ads.identifier.service.START` | Preserve; provider uses the original action |
| `com.google.android.gms.measurement.START` | Provider support requires a separate check |

The shared **MicroG integration** patch includes these exact obfuscated availability methods. It redirects selected external package, account, service and provider identifiers to `app.revanced.android.gms`, adds a package query and original signer metadata, and makes both availability checks return success. Class/Binder descriptors, app-local provider authorities, internal Firebase actions and Play Billing routing retain their original values. The unsupported or unverified service actions above also remain unchanged.

The patched base APK and supplied splits installed on an isolated Android 37 emulator and reached onboarding/`MainActivity` offline without a package crash. The patch requires the matching MicroG provider for Google services; FCM delivery, account registration and paid feature behavior remain unverified. See [validation results](validation.md).

## Validation

The local-controls and MicroG patches compiled with Kotlin 2.4.21 and Morphe Patcher 2.1.0, applied to the original base APK, and produced patched DEX and resources. SDK DEX verification passed with no hierarchy findings. Protocol-routing tests also passed. The packaged `.mpp` passed loader checks; the full Gradle task graph and device behavior remain pending. See [validation results](validation.md).
