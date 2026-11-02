# Research patch validation

Validated on 2026-11-02 against the base APK hashes recorded in each app's research report. Original workspace archives were preserved. No patched APK was installed and no live purchase or entitlement endpoint was contacted.

## Completed

All patch sources and test sources compiled using cached Kotlin 2.4.21 and Morphe Patcher 2.1.0. The renshuu extension compiled to DEX using Android build-tools 36.0.0. Two JUnit checks passed:

- Pro-state checks cover existing flags, guest and sparse data, null input, repeated application, and preservation of account and schedule data.
- MicroG routing checks cover selected protocol identifiers, provider URI boundaries, manifest actions, and preservation of class/Binder descriptors, internal Firebase actions, Play Billing and unsupported service actions.

The actual Morphe Patcher applied the selected patches and compiled their output:

| App | Selected patches | Output | SDK hierarchy findings before / after |
| --- | --- | --- | --- |
| renshuu 1.2.20260311 | Enable Pro menus; MicroG integration | DEX and resources | 176 / 176 |
| Du Chinese 3.6.1 | Enable local Premium controls; MicroG integration | DEX and resources | 0 / 0 |
| TEUIDA 1.23.0 | Enable local Premium controls | DEX | 1,938 / 1,938 |
| Pimsleur 8.1 | MicroG integration | DEX and resources | 58 / 58 |

SDK verification ran on both original and patched DEX. Its optional-dependency hierarchy findings were compared by exact normalized content, retaining duplicates; any added or changed finding failed the check. This comparison does not establish runtime correctness. Du Chinese passed the verifier without findings.

Checks of compiled DEX confirmed the renshuu helper invocation and register, Du Chinese's single next-lesson result override and unchanged global lesson-lock getter, TEUIDA's three getter overrides, and both availability overrides for each MicroG target. Selected Binder, Firebase and Play Billing literals retained their original counts.

Compiled manifest checks confirmed the MicroG package query, receiver action and sender permission, preserved Firebase and Billing declarations, and spoofed package/signature metadata. Android `apksigner` verified each MicroG target's original base APK signature; the inserted SHA-1 metadata matched its certificate. This verifies internal signature consistency, not the publisher's identity. Pimsleur's Hermes asset hash remained unchanged.

Local validation scripts, compiler output, patch application logs and extracted artifacts are retained under `/private/tmp/renshuu-research/`. These private files are not part of the public repository.

## Pending

Full Gradle `.mpp` packaging remains pending: the agent sandbox denies the local socket Gradle uses for cache locking. The source compilation and APK application checks above ran directly with cached tools.

Run from the repository in a terminal that permits Gradle:

```sh
export GITHUB_ACTOR=MangoTornado
export GITHUB_TOKEN="$(gh auth token)"
export ANDROID_HOME="$HOME/android_sdk"
./gradlew buildAndroid :patches:test
```

Device checks require the original splits, a compatible `app.revanced.android.gms` installation for the three MicroG targets, and developer-provided test accounts. Verify startup, local feature behavior, server-controlled content, login, cancellation, sign-out, FCM registration/delivery, and billing purchase/restore behavior. Static local overrides do not establish server entitlement or content access.

Patch compatibility checks package and version name; it does not require a matching APK hash or version code. The hashes in the reports identify the tested inputs.

TEUIDA MicroG requires additional Credential Manager changes described in its report. Pimsleur Premium requires recovered Hermes control flow. Kanji Study requires a diagnostic build or corresponding sources and mapping.
