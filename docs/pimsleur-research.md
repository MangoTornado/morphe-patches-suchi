# Pimsleur static research

Inspected on 2026-11-02 with REA and its JADX provider, reported version `0.7.1`. The original workspace archive was preserved. Only its extracted base APK was analyzed; split APK behavior, publisher identity, native libraries, device behavior and backend enforcement were not verified. APK signature verification passed during output checks.

| Artifact | Value |
| --- | --- |
| Package | `com.simonandschuster.pimsleur.unified.android` |
| Version | `8.1`, version code `759` |
| Android SDK | Minimum `24`, target `36` |
| Archive SHA-256 | `290e073e95492c70008bc7d8fd0ea874a7a6cccd20547d165cdcd6db5ec5a065` |
| Base APK SHA-256 | `15629eef999249cd02e79e8af056df9272551ce82126993480130811a68ffef9` |

## Premium and billing

The native class inventory contains React Native application classes and Pimsleur bridges for authentication, media, encryption and Android Auto. Local ZIP inspection also found the 8,269,940-byte `assets/index.android.bundle`; its first twelve bytes are `c6 1f bc 03 c1 03 19 1f 60 00 00 00`, consistent with Hermes bytecode version 96. This bundle was extracted but not decompiled. The bounded native investigation did not establish a local Premium predicate, so no Premium patch is provided.

The base APK packages Google Play Billing, Nitro React Native IAP and OpenIAP classes, including active-subscription and receipt-validation models. Their presence does not establish which purchase path runs or whether a server verifies entitlement. `NativeLoginModule.exchangeTokens` calls Auth0's native social token exchange with an audience and scope. That is evidence of a remote authentication path, not proof of Premium validation.

To establish a patchable gate, inspect the developer's matching JavaScript source or the Hermes bytecode with a compatible decoder, then follow the entitlement response into the actual content decision. A developer test account and controlled runtime capture would distinguish locally gated content from server-controlled downloads.

## Hermes follow-up

The extracted bundle's SHA-256 is `00de65410c84a4907be54bfe11d211b8a7f5e868264f7e924af9398131fec0c5`. Its trailing 20 bytes match the SHA-1 of the preceding bytes, consistent with the footer checked by the workspace's Morphe Hermes helper.

No `hermes`, `hermesc`, `hbcdump`, `hermes-dec` or `hbc-disassembler` executable was found on the current PATH; bounded workspace and installed-tool searches found no compatible decoder. REA `6.7.0` advertises source JavaScript and PE/CLI managed analysis, but no Hermes bytecode decoder. Its source JavaScript recovery tool expects UTF-8 JavaScript and cannot decode this binary bundle. The existing Morphe `hermesPatch` helper validates magic/version, replaces supplied byte patterns and repairs the footer; it does not recover string tables, function tables or instructions.

Raw byte searches found these useful anchors. Offsets are decimal bytes from the bundle's beginning. These are substrings in packed storage, not decoded string entries; adjacent bytes can belong to unrelated strings.

| Raw substring | Byte offset |
| --- | ---: |
| `@@purchase/verifyReceiptViaBackend` | `1401946` |
| `Products.entitlementExpired` | `1447722` |
| `checkEntitlementAfterExternalPurchase` | `1797518` |
| `getSubscriptionStatus` | `1836010` |
| `noEntitlementExpiredPopup` | `1931045` |
| `hasAccessToAiProduct` | `2009857` |
| `hasActiveSubscriptions` | `2009877` |
| `isSubscribed` | `2018550` |

These names suggest useful purchase and access-state investigation points, but no consuming function or branch was recovered. They do not establish a local gate, a receipt-validation request, server enforcement or an active execution path. The next required input is a Hermes **version 96-compatible** reader that exposes string IDs, function headers and instruction operands, or the developer's matching `8.1` JavaScript/Metro source map. An entitlement response schema and expected free/paid behavior would help interpret the recovered decision. No tool was installed and no bundle bytes were changed.

## MicroG applicability

Both named availability methods are present with the exact DEX prototype `(Landroid/content/Context;I)I`: `GooglePlayServicesUtilLight.isGooglePlayServicesAvailable` and `GoogleApiAvailabilityLight.isGooglePlayServicesAvailable`. The wrapper delegates to the utility and maps an updating condition to status `18`. JADX reports duplicated blocks in the utility body, so its reconstructed branch details should not be used as exact control-flow evidence.

`FirebaseMessaging.getToken` has a native caller in Salesforce Marketing Cloud's `MCService.d(Context,String)`. This makes native messaging a candidate for service-package redirection, subject to device testing. A bounded trace of `GoogleSignInClient.getSignInIntent` found only an SDK-internal caller; it did not establish an application Google sign-in route. JavaScript, dynamic and reflective paths remain unknown. Google Play Billing's `com.android.vending` route must stay intact when testing messaging changes.

The shared **MicroG integration** patch targets only inspected version `8.1`, marked experimental and disabled by default. It redirects selected native external Google package, account, service and provider identifiers to `app.revanced.android.gms`, adds its package query and original signer metadata, and makes both availability methods return success. It leaves the Hermes bundle, class/Binder descriptors, internal Firebase actions and Play Billing routing unchanged. A matching MicroG installation is required. This native routing does not establish JavaScript compatibility, successful login or notification delivery.

## Validation

The MicroG patch compiled with Kotlin 2.4.21 and Morphe Patcher 2.1.0, applied to the original base APK, and produced patched DEX and resources. SDK DEX verification found the same 58 optional-dependency hierarchy findings in the original and patched APK; no findings were added. Protocol-routing tests passed. Full Gradle bundle packaging and device testing remain pending; see [validation results](validation.md).

## Evidence

Canonical records are exported locally at `/private/tmp/pimsleur-research/evidence.json`; extracted identity and bytecode are in that directory. Supplemental ZIP, byte-level and tool-inventory observations are recorded in `/private/tmp/pimsleur-research/hermes-raw-findings.json`, separate from the REA records.

| Observation | REA evidence ID |
| --- | --- |
| Package identity | `ev_d2e7575130977ebb1ed86901d386a2e5629e91c7ffb404f2e26780e39aa27fb6` |
| Native app inventory | `ev_a15c101561d7a5a74ddc56d00bc4fb261fca326455f85fa4243c3bbe3a74ea78` |
| Auth0 token exchange | `ev_c3915b84e145bbc78586ff574893ddad07ae7b7656eebb238fcd5eff62ce316c` |
| Native IAP inventory | `ev_94b8f75ca3f41cd5f74497b95896e3953848df4bc7c76a91c20a36cf913c5c14` |
| FCM caller | `ev_f866284b4f634f08898ad7b6e53022b34d934ecfe73c5018556205176f67f2b3` |
| Google sign-in caller trace | `ev_d5c55f48672fd5fc282402d6760fb70392d18b8e176e7b5b920f113582cb5b0f` |
| Utility availability method | `ev_232d1b07d81319e925ad89eca945006afd06ef1c7a9e7a26dec5bf07a4cb6913` |
| API availability wrapper | `ev_ffe4c81f116b55e1464fc956e0536ebfe0ef6c6f8d8f5df6a4687e96c7f248e8` |
