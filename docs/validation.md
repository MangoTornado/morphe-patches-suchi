# Research patch validation

Validated on 2026-11-02 against the base APK hashes recorded in each app's research report. Original workspace archives were preserved. No patched APK was installed and no live purchase or entitlement endpoint was contacted.

## Completed

All patch sources and test sources compiled using cached Kotlin 2.4.21 and Morphe Patcher 2.1.0. Two JUnit checks passed:

- Pro-state checks cover existing flags, guest and sparse data, null input, repeated application, and preservation of account and schedule data.
- MicroG routing checks cover selected protocol identifiers, provider URI boundaries, manifest actions, and preservation of class/Binder descriptors, internal Firebase actions, Play Billing and unsupported service actions.

A fresh local `patches/build/libs/patches-1.0.0.mpp` was built through the documented Gradle task graph with Java 21, Kotlin 2.4.21 and Morphe plugin 2.1.0. `buildAndroid`, `:patches:test` and `generatePatchesList` passed, with two tests and no failures or skips. Java compilation explicitly targets release 17. The Java-only renshuu extension excludes the unused Kotlin runtime.

Morphe Patcher 2.1.0 loaded the canonical bundle and found five named, disabled-by-default patches, including Pimsleur's raw-resource patch and four MicroG targets. The patch DEX contains 42 classes and the renshuu extension contains its `ProState` helper and generated Android `R` class, with no test, template or third-party app classes.

The plugin signed the canonical bundle with Ed25519, storing its signature in the ZIP comment. Morphe Patcher verified its signer against `morphe-signing.pub` and rejected a copy with a changed byte. The signed bundle is 88,971 bytes; SHA-256 `76a6bb17c658708609670a051e7e51d115ba3533041a076be0e96fc586d7aded`. The signing-key fingerprint is `898a 6e74 45aa eec8`. The private key is ignored by Git and backed up outside the repository with restricted file permissions; uploading it to the Actions secret remains pending.

The existing `generatePatchesList` task produced a valid patch list: five named patches, all disabled by default, and four MicroG targets. Generated release files and the README's generated section remain reserved for `release.yml`. CI now runs tests before semantic release and requires the signing public key and Actions secret. The pinned Morphe changelog plugin's inspected source confirms that it publishes the public key as `public_key` in `patches-bundle.json`; the detached signature URL remains empty for this inline-signature format.

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

Local validation scripts, compiler output, patch application logs and extracted artifacts are retained under `/private/tmp/renshuu-research/`, `/private/tmp/teuida-research/`, `/private/tmp/pimsleur-research/`, `/private/tmp/suchi-gradle-unblock/final-checks/` and `/private/tmp/suchi-release-audit/`. These private files are not part of the public repository.

## Pending

Installed AVDs were found in `~/.android/avd/`, with the emulator and ARM64 system images in `~/Library/Android/sdk/`. An isolated test AVD preserved the original AVD data. The sandboxed attempt exited before boot with `SIGILL` in `init_cache_info`; the recorded instruction is `mrs x9, CTR_EL0`. Static inspection connected that fallback to a denied `hw.cachelinesize` query. After execution outside the sandbox was enabled, the query returned 128, Android booted, and an isolated loopback ADB server connected successfully. Emulator and ADB listeners were checked to remain on loopback. The original failure evidence is retained in `/private/tmp/suchi-emulator-investigation/investigation.json`; the successful boot and listener checks are under `/private/tmp/suchi-emulator/`. App smoke checks remain pending.

Device checks require the original splits, a compatible `app.revanced.android.gms` installation for the four MicroG targets, and developer-provided test accounts. Verify startup, local feature behavior, server-controlled content, login, cancellation, sign-out, FCM registration/delivery, and billing purchase/restore behavior. Static local overrides do not establish server entitlement or content access.

Patch compatibility checks package and version name. Pimsleur's Premium patch additionally requires the exact inspected Hermes asset hash; the other patches do not require a matching APK hash or version code. Report hashes identify the tested inputs.

Kanji Study's protected records and secondary DEX were recovered for inspection. Its report identifies a local upgrade preference, purchase and feature consumers, server-response checks, and an active Drive OAuth2 path. Those methods remain outside the original base DEX, and re-signing changes inputs to the protector's authenticated key derivation. A deployable patch requires reconstructing protected-record rewriting and loader integration, or a developer build that supports test signing. No Kanji Study patch is included.

GitHub authentication and fetch now work outside the sandbox. The repository currently has no signing secret, and Actions pull-request approval is disabled. Uploading the newly generated private key and enabling that setting are awaiting explicit approval after automatic approval review rejected those changes. The remaining configuration and publication steps are:

```sh
gh secret set MORPHE_SIGNING_KEY --repo MangoTornado/morphe-patches-suchi < morphe-signing.key
gh api -X PUT repos/MangoTornado/morphe-patches-suchi/actions/permissions/workflow \
  -F can_approve_pull_request_reviews=true
git push origin dev
```

After confirming the secret was uploaded, keep the private backup and delete the ignored repository key. A `feat:` or `fix:` commit lets the existing workflow publish a signed development release, generate `patches-list.json` and `patches-bundle.json`, and update the README patch list. Development releases require enabling pre-release in Morphe Manager. Merge `dev` into `main` without squashing for a stable release.
