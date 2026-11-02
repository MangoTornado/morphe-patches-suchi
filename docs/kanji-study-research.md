# Kanji Study research

Static inspection on 2026-11-02 using REA, JADX 0.7.1 and Hopper. The app was not
executed and no service requests were sent.

## Artifact

| Field | Value |
| --- | --- |
| Supplied archive | `Kanji Study.apks` |
| Package | `com.mindtwisted.kanjistudy` |
| Version | `7.8.3` (`70803`) |
| SDK declarations | Minimum 32; target 37 |
| Archive SHA-256 | `731299d253429c33735fb53e89fd0ff71e4e0a5e1865edadda5e6f87ca3b5fef` |
| Base APK SHA-256 | `0231ab6e6a6cdb4140439ebfa4fb2f9bafd4aea7fb321018ae3586efec5ebca8` |

REA package evidence: `ev_cc841f335952fd4a35038ba19430ac70db7c817e9543bc6b746dd88362ef5632`.
The archive also contains arm64 and density splits. Package declarations do not
verify the publisher's signature.

## Observations

- `ProtectedCustomApplication.attachBaseContext` calls a certificate check
  before loading `dexprotector`. The check rejects missing or multiple signing
  certificates and requires certificate SHA-256
  `3759e71d7e93d41ab27e36514085eadff025b0f36e1d25513a9e296c694f122b`.
  Evidence: `ev_dbbbb743a8ab9958749ae0d6ad15e064e177114d1c84d74566838d5e12e5ee27`,
  `ev_f804026bc6317b71a3ecf99e5714fa706f0e92582d4411713a86515e45803a91`.
- The application exposes native string and initialization methods. A sampled
  boolean dispatcher in `LibCustomApplication` also has no DEX body. This
  establishes a native boundary without identifying an entitlement predicate.
  Evidence: `ev_560573ec08d8a2dcc6e1c08a2d6870883546106183284cd7302a39b35b92a9cc`,
  `ev_7b9f26dc80a00d1f2157fc755a72d2c2288a6386165e8b96312a4e4f7c698562`,
  `ev_482341fd92a9996fbac0d066ac7e20126cef1f5f8f1497ba752d0f1433b98fed`.
- A complete top-level JADX class-name search for `Purchase` returned no matches.
  A separate raw DEX inventory found purchase activity type references without
  corresponding class definitions. No precise local purchase gate was recovered.
  Evidence: `ev_54806fcc74a12251d941d5deb95169248fbf01807247c5036d6cf02a2f3398d0`.
- The package declares Play Billing and C2DM permissions. Google sign-in account
  and options classes are present, but that inventory alone does not establish an
  active sign-in path. No class-name match for `GoogleApiAvailability` was found.
  Evidence: `ev_8d2c3324d924a0b473835de729eedc1f3e8a63c5c7bd1eb9b8050efa1b95d885`,
  `ev_2bc9d464b7396e6cd8f0e74dc2bf672b11e88879bc4fbd0c3275d748f4216d65`.
- `NotificationSettingsFragment.onSharedPreferenceChanged` calls Firebase
  Messaging when a notification preference changes. This is an app-owned FCM
  entry point, although token delivery was not tested.
  Evidence: `ev_fda1e22cf814c0c992510605aed6275423b52d637c4c37de30d17c6cfaa6bbef`.
- The arm64 split contains `libdexprotector.so`, SHA-256
  `feb3a7dc60da870000a2556fe7122889f87c8ebcf6d85d64ab08961a4ce1771a`.
  Hopper identified 31 procedures and 17 typed string objects. `JNI_OnLoad`
  dispatches through an indirect pointer; this bounded inspection did not recover
  the protected application logic.
  Evidence: `ev_63e1149ed5f78f4eba12a3490d51aeeb6d57f98d6f6c68e11075100c46da8cd0`,
  `ev_5bf2d27ff47301a19450f1c4bb05435d90c253249aaff1d8b15ae1acf79702d9`.

## Patch decision and remaining verification

No premium or MicroG patch is included for this artifact. Re-signing with a
different certificate would fail the observed Java startup check. Further
native checks, protected entitlement logic and server validation remain unknown.
The renshuu MicroG fingerprints also cannot be applied directly to this obfuscated
class inventory.

A developer-provided unprotected diagnostic build or corresponding sources and
mapping would allow the purchase callback and entitlement consumers to be traced.
Use free and entitled test accounts to compare the upgrade, Guided Study, KLC and
Outlier paths. For MicroG, verify the actual sign-in API, availability checker,
FCM registration and delivery on a test device after establishing that the
diagnostic build supports re-signing.

The private evidence ledger and native snapshot are retained in
`/private/tmp/kanji-study-research/`. No APK, content database or decompiled app
source is included in this repository.
