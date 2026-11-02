# Research patch validation

Validated on 2026-11-02 against the base APK hashes recorded in each app's research report. Original workspace archives were preserved. No patched APK was installed and no live purchase or entitlement endpoint was contacted.

## Completed

All patch sources and test sources compiled using cached Kotlin 2.4.21 and Morphe Patcher 2.1.0. Two JUnit checks passed:

- Pro-state checks cover existing flags, guest and sparse data, null input, repeated application, and preservation of account and schedule data.
- MicroG routing checks cover selected protocol identifiers, provider URI boundaries, manifest actions, and preservation of class/Binder descriptors, internal Firebase actions, Play Billing and unsupported service actions.

A fresh local `patches/build/libs/patches-1.0.0.mpp` was built with the cached compiler and the Gradle plugin's D8 API and settings: D8 9.1.31 from AGP builder 9.1.0, JVM target 17, extension minimum API 23 and bundle minimum API 26. Morphe Patcher 2.1.0 loaded the bundle and found five named, disabled-by-default patches, including Pimsleur's raw-resource patch and four MicroG targets. The patch DEX contains 42 classes and the renshuu extension contains one, with no test, template or third-party app classes. The bundle is 89,211 bytes; SHA-256 `e1f27a96fb60da770583d4d8f463a28924d2c34bd53365acb94597cfad73235e`. This build did not execute Gradle's task graph.

The actual Morphe Patcher applied the selected patches and compiled their output:

| App | Selected patches | Output | SDK hierarchy findings before / after |
| --- | --- | --- | --- |
| renshuu 1.2.20260311 | Enable Pro menus; MicroG integration | DEX and resources | 176 / 176 |
| Du Chinese 3.6.1 | Enable local Premium controls; MicroG integration | DEX and resources | 0 / 0 |
| TEUIDA 1.23.0 | Enable local Premium controls; MicroG integration | DEX and resources | 1,938 / 1,938 |
| Pimsleur 8.1 | Enable local Premium controls; MicroG integration | DEX, resources and Hermes asset | 58 / 58 |

SDK verification ran on both original and patched DEX. Its optional-dependency hierarchy findings were compared by exact normalized content, retaining duplicates; any added or changed finding failed the check. This comparison does not establish runtime correctness. Du Chinese passed the verifier without findings.

Checks of compiled DEX confirmed the renshuu helper invocation and register, Du Chinese's single next-lesson result override and unchanged global lesson-lock getter, TEUIDA's three getter overrides, and both availability overrides for each MicroG target. TEUIDA's checks also verified four credential type literals and the encoded field constant, the non-system provider flag, preserved auto-select and empty provider set, and selection of the existing older sign-in controller. Selected Binder, Firebase and Play Billing literals retained their original counts.

Compiled manifest checks confirmed the MicroG package query, receiver action and sender permission, preserved Firebase and Billing declarations, and spoofed package/signature metadata for all four targets. Android `apksigner` verified each target's original base APK signature; the inserted SHA-1 metadata matched its certificate. This verifies internal signature consistency, not the publisher's identity.

Pimsleur's actual patcher output changed 128 bytes, confined to the two selector bodies and SHA-1 footer. The 37,658 other distinct function bodies, all headers and all tables remained unchanged; both replacements decoded correctly and the footer verified. Its asset SHA-256 matched `30a659353cf17666a027c038ea47262427ede69c2d15992ceebcc561379a7cb4`. A separate actual patcher run rejected a changed Hermes bundle of the same app version before mutation. The modified asset is exported in `other-resources/assets/index.android.bundle` alongside compiled resources for APK assembly.

Local validation scripts, compiler output, patch application logs and extracted artifacts are retained under `/private/tmp/renshuu-research/`, `/private/tmp/teuida-research/`, `/private/tmp/pimsleur-research/` and `/private/tmp/suchi-gradle-unblock/final-checks/`. These private files are not part of the public repository.

## Pending

The full Gradle task graph remains pending: the agent sandbox denies the local socket Gradle uses for cache locking, before project configuration. The source compilation, bundle packaging and APK application checks above ran directly with cached components.

Run from the repository in a terminal that permits Gradle:

```sh
export GITHUB_ACTOR=MangoTornado
export GITHUB_TOKEN="$(gh auth token)"
export ANDROID_HOME="$HOME/android_sdk"
./gradlew buildAndroid :patches:test
```

Installed AVDs were found in `~/.android/avd/`, with the emulator and ARM64 system images in `~/Library/Android/sdk/`. An isolated test AVD preserved the original AVD data. Its emulator exited before boot with `SIGILL` in `init_cache_info`; the crash report records instruction `0xd53b0029` (`mrs x9, CTR_EL0`). Static inspection found the same initializer in all four installed QEMU variants: a failed or zero `hw.cachelinesize` query reaches this instruction before `main`. The identical read-only query was denied here, consistent with sandbox denial triggering the observed fallback. Graphics, UI and acceleration flags cannot change that initializer. Evidence is retained in `/private/tmp/suchi-emulator-investigation/investigation.json`. Separately, ADB could not create its local server listener (`Operation not permitted`). No emulator or device test completed.

Device checks require the original splits, a compatible `app.revanced.android.gms` installation for the four MicroG targets, and developer-provided test accounts. Verify startup, local feature behavior, server-controlled content, login, cancellation, sign-out, FCM registration/delivery, and billing purchase/restore behavior. Static local overrides do not establish server entitlement or content access.

Patch compatibility checks package and version name. Pimsleur's Premium patch additionally requires the exact inspected Hermes asset hash; the other patches do not require a matching APK hash or version code. Report hashes identify the tested inputs.

Kanji Study's protected app logic remains under investigation; its report records recovered loader layers and the remaining encrypted payload. A developer diagnostic build or runtime dump on an original-signed installation is an alternative to continued static recovery.

Publishing to `origin/dev` is blocked by shell DNS resolution for GitHub. GitHub's authenticated workflow permission setting could not be checked. From a terminal with GitHub access, the remaining publication steps are:

```sh
git push origin dev
gh api -X PUT repos/MangoTornado/morphe-patches-suchi/actions/permissions/workflow \
  -F can_approve_pull_request_reviews=true
```
