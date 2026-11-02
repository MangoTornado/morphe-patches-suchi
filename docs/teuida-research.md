# TEUIDA static research

Inspected on 2026-11-02 with REA and its JADX provider, reported version `0.7.1`. The original workspace archive was preserved. Its extracted base APK passed signature verification during output checks; publisher identity and backend enforcement remain unverified. Offline split-APK startup and Pairip protection checks are recorded in [validation results](validation.md).

| Artifact | Value |
| --- | --- |
| Package | `net.teuida.teuida` |
| Version | `1.23.0`, version code `939` |
| Android SDK | Minimum `32`, target `36` |
| Archive SHA-256 | `9426d84df488db554f6546b4748d2689d12c34af0ced843dc3dfdb24356003e4` |
| Base APK SHA-256 | `0d1347c54f1b5563bf586d7a7bb77ae95f0d828778d65072ba73f77407d503aa` |

## Local Premium controls

`UserShareds.l()Z` reads `MeData`'s `premium` value when a profile is present. Otherwise it reads a cached Boolean or the `premium` preference, defaulting to false. The profile's raw DEX getters are `MeData.s()Ljava/lang/Boolean;` and `MeData.t()Lnet/teuida/teuida/enums/PremiumState;`, returning the `premium` and `premiumState` fields. JADX displays these getters as `getPremium` and `getPremiumState`; patch fingerprints use the raw names.

The incoming trace identified 37 containing nodes for `UserShareds.l`, including curriculum, story, lesson, content and advertising code. `PremiumExtKt.b` provides a concrete consumer: the false branch displays a Premium access prompt, while the true branch displays a story welcome prompt.

The experimental, disabled-by-default **Enable local Premium controls** patch targets only version `1.23.0`. It makes these three getters return `true`, `Boolean.TRUE` and `PremiumState.PREMIUM`. It validates the two profile field references before mutation. The patch does not write profile fields, preferences, purchase receipts or server-side account entitlement.

`PremiumRepositoryImpl.h` sends a `SendPurchaseSubscriptionRequest` through `TeuidaAPI.V` with authentication headers. A separate backend purchase path therefore exists. Static inspection does not establish its validation rules, whether downloaded content has further checks, or the behavior of inconsistent trial, gift and expiry fields. Device testing with developer-provided free and entitled accounts is required.

## MicroG applicability

Both named availability methods exist with the exact DEX prototype `(Landroid/content/Context;I)I`: `GooglePlayServicesUtilLight.isGooglePlayServicesAvailable` and `GoogleApiAvailabilityLight.isGooglePlayServicesAvailable`. The wrapper delegates to the utility and maps an updating condition to `18`. The utility's JADX output contains duplicated-block warnings; precise branch conclusions require DEX inspection.

The app's `GoogleSignInHelperKt.b` uses **Credential Manager**, `GetSignInWithGoogleOption`, `GetCredentialRequest` and `GoogleIdTokenCredential`, then passes the ID token to Firebase Authentication. Legacy `GoogleSignInClient` redirection alone does not establish compatibility with this path. Credential provider discovery and routing need separate verification against the installed MicroG variant and Android version.

Native messaging is app-used: `FirebaseMessaging.getToken` has callers in `net.teuida.teuida.fcm.FcmTokenProvider.a` and `tech.notifly.utils.NotiflyFirebaseUtil.getFcmToken`. Messaging can be researched independently of the unresolved Credential Manager route. Google Play Billing's `com.android.vending` route must stay intact.

The experimental, disabled-by-default **MicroG integration** candidate now includes TEUIDA `1.23.0`. It maps the exact Google credential type to the MicroG provider capability, permits a non-system provider only for the Google sign-in option, and preserves the older `getSignInIntent` route in the Play Services fallback. Google ID bundle keys and Google Play Billing routing stay intact. On Android API 34+, the user must enable the MicroG Google ID credential provider. The [Credential Manager follow-up](teuida-microg-research.md) records the exact hooks and remaining device-verification requirements.

## Validation

The combined local Premium + MicroG candidate compiled with Kotlin 2.4.21 and Morphe Patcher 2.1.0, applied to the original base APK, and produced patched DEX and compiled resources. SDK DEX verification found the same 1,938 optional-dependency hierarchy findings in the original and patched APK; no findings were added. Separate compiled-output checks verified the credential type constant/literals, provider flags, older backend selection, and unchanged Google ID bundle keys. Compiled manifest and signer metadata checks passed. The packaged `.mpp` passed loader checks; the full Gradle task graph and device testing are tracked in [validation results](validation.md).

## Evidence

Canonical records are exported locally at `/private/tmp/teuida-research/evidence.json`; the base APK and identity file are in that directory. No app account or live purchase service was used. The later offline emulator checks are recorded separately in [validation results](validation.md).

| Observation | REA evidence ID |
| --- | --- |
| Package identity | `ev_1cd50e14b498f5c041acb3a8a8a3dde668a181923c0faf53d273799eefaf061a` |
| Shared Premium getter | `ev_067c885e1b4145e417806a41183fd2fe312c7b0ee7728a3155280fa9edfee4e3` |
| Profile Boolean getter | `ev_cc0f2567a65c2b506a84d4586ab6bd30089c9131cb878c5b97af2e804dc10488` |
| Profile state getter | `ev_d201b3b2ff6a5783544e78df4232e609c0da6a9583e3a1f584d136d7cf0cc95c` |
| Story prompt consumer | `ev_2aa7eb70a32691a712401d8c27701580993b4655a41e9c54dc36cc610a6e0017` |
| Shared getter callers | `ev_96966114499ba51d6fe7f52b0f66614abdf7d4668d31b4290968fc90fef3fd44` |
| Backend purchase request | `ev_28070274a61fa288e658118a5fc25c691235175a7fc9edfe46cab7a4bf833f2a` |
| Credential Manager sign-in | `ev_cc2de6706c7764393ce18d35bab9bdb721a4cf25be514ce77a1ed8e7949e0037` |
| Utility availability method | `ev_d23dd0a715fbe998a0280433fa3c7988966385f1c534a1a1e60a64f588d1a39b` |
| API availability wrapper | `ev_287b4c1174a2c019ae68e160681ffd75240961520c990d0b356ea79d500dd4af` |
| FCM callers | `ev_7f870da3f53114300ad7cbc6ef84ca7a81ca7d0440757a678d0b5a6e6199bb6b` |
