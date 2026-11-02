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
The archive also contains arm64 and density splits. Independent SDK
`ApkVerifier` inspection verifies all three APK signatures and confirms the same
package, version code and single signing certificate on each split. The signer
SHA-256 is
`3759e71d7e93d41ab27e36514085eadff025b0f36e1d25513a9e296c694f122b`.
The private verification results are retained with the recovery inputs.

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
- A complete top-level JADX class-name search of the original base APK for
  `Purchase` returned no matches. Its raw DEX inventory contains purchase
  activity type references without their class definitions. The later static
  asset recovery described below exposes their method bodies.
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
  dispatches through an indirect pointer. Its ordinary section inventory omits
  a 432,480-byte encrypted fourth load segment.
  Evidence: `ev_63e1149ed5f78f4eba12a3490d51aeeb6d57f98d6f6c68e11075100c46da8cd0`,
  `ev_5bf2d27ff47301a19450f1c4bb05435d90c253249aaff1d8b15ae1acf79702d9`.

## Protected payload recovery

Static recovery reached a second native layer without executing the app. The
outer constructor at `0x380` selects the fourth ELF load segment and invokes a
key bytecode interpreter, a counter-mode decryptor and an LZ4 decoder. A local
Python reproduction completed 339,384 interpreter steps and recovered four
segments. Their declared sizes and all four native rolling checksums match.
The decrypted container accounts for every byte of the original load segment.

| Recovered virtual address | Inflated bytes | Memory bytes | SHA-256 of inflated segment |
| --- | ---: | ---: | --- |
| `0x0` | 615136 | 615136 | `2ffb596279ade39f847206de88915f3bf01bb01b6db7a5edd7f360bd65241407` |
| `0x9a2e0` | 17192 | 17192 | `f9102d9c2e3df3984263055895bc964ab398a1f0a8642ae45d2fe5277d3ea413` |
| `0xa2610` | 5284 | 24840 | `064fcdb7fb5b0eb85a18a66987c8fba9c1a68d04f8ccef7a32f9dcc2f8e19597` |
| `0xac000` | 792 | 792 | `52273691907e71c5e69fea11577083333c4fc4dfb5d2269c1e6f5d99463625f1` |

The recovery uses the original AArch64 return instruction `c0035fd6` as the
outer loader's linker callback input. The successful checksums support that
choice; it was not guessed from readable strings alone. The analysis ELF adds
headers and labels to the recovered memory image and is not an executable APK
component. The separate raw image preserves the original header bytes for
subsequent integrity calculations.

Outer-layer REA evidence:
`ev_92776a1d11683a5ad0b4c746fb3c893145545fc1e729f8403f27a8ed76d6410f`
(constructor bytes),
`ev_df53e0e0b5324ef74042300bfbbe614f312067ffc06d7d765962eacfc1ff1163`
(key interpreter),
`ev_bcc2832a9b7274cbfe457bc995083b2d93ec0a90ef5fc67a8a46267c9b160c39`
(counter decryptor),
`ev_b307c06ac71702755482aea34d90731bb699088ed5be9f4b57e69539f3733b82`
(LZ4 decoder).

The recovered native entry is `0x39f00`. Its string decoder identifies
`assets/assets.dat`, `classes.dex`, `dalvik/system/DexFile` and Android asset
APIs. A partial inventory decoded 64 distinct string sites. The asset is
2,388,612 bytes with SHA-256
`58b68386635ac356cdadeb0211aa78d83563162f47abb615bc4bb88d18f6e85b`.
It contains an encrypted record directory: 26 bytes of ciphertext, a 16-byte
authentication tag, and the final byte `0x1a`, which supplies both the directory
length and its one-byte authenticated data. Directory records store a type byte
and a variable-length size.

The inspected cryptographic routines implement HMAC-SHA-256 HKDF,
ChaCha20-Poly1305 and padded AES key unwrap. Active flags select APK
certificate and `classes.dex` integrity mixing before the directory key is
derived. A further input hashes the recovered native header and code.
Certificate-derived and native-code-derived unwraps both authenticate:
the native calculation reproduces the outer loader's removal of symbol and
relocation tables before hashing. No device-bound or server-provided secret has
been established.

The pre-extraction native path flips seed byte `0xa287f` from `0x15` to `0x14`.
Instructions `0x47374`–`0x47380` implement the XOR by one; the surrounding
function receives the seed base `0xa2848`. Reproducing that change produces the
embedded four-byte key check and authenticates the directory. The seven
directory records account for all 2,388,569 bytes preceding the trailer:

| Record type | Bytes | Inspected native consumer |
| --- | ---: | --- |
| 9 | 1740 | `0x47b30`: authenticated bootstrap DEX |
| 5 | 1401974 | `0x78e20`: encrypted, compressed DEX container |
| 4 | 188765 | `0x7ac04`: protected class/member metadata |
| 1 | 771554 | `0x419c8`: authenticated protected execution data |
| 10 | 158 | `0x42e54` |
| 8 | 23644 | `0x4e774`: authenticated alternate execution data |
| 6 | 734 | `0x485b0`: authenticated resource-name table |

Type 9 decrypts to a valid 1,724-byte DEX (`035`) with SHA-256
`b27d3b369b80395fd8082249deec6512f114ea47c3df2b420a5cd3c2358f21fc`.
It contains the loader's bootstrap class rather than the missing purchase
activities. Type 6 decrypts and decompresses with Zstandard to a 3,740-byte
table containing 138 names, including protected assets and `classes.dex`.
Type 8 also authenticates and its internal length matches its 23,628-byte
plaintext.

The bootstrap's byte-return native callback supplies a fixed 32-byte literal.
Its DEX transforms those bytes through the application substitution tables and
integer operations. Independent raw DEX instruction interpretation reproduces
the same output as the decompiled arithmetic. Mixing that output into the
recovered root enables the remaining application record decoders. Type 5 uses
the loader's eight-round ChaCha stream variant followed by Zstandard; types 4
and 1 authenticate with ChaCha20-Poly1305. Their recovered sizes and hashes are:

| Record | Plaintext bytes | SHA-256 |
| --- | ---: | --- |
| 5 | 4096276 | `f89ad8c7fc8074596a7279dd041132a0103b73a7b2aabc0f3ca6331de67bc0ed` |
| 4 | 562386 | `69fa990f15a7818c1c5bbec51b4888f4bcc6ef4f5b2b544fe0ec10b51950fb79` |
| 1 | 771538 | `5227b1370b4ba5dcaf7b17161bb24fbe8335e41e4c9ae9ffbe99ff2abe36fb66` |

Record 4 contains 2,775 class names and 18,818 member records. Native consumers
at `0x3bbb4`, `0x3bd70` and `0x3c084` establish that each member's first word,
shifted right by ten, indexes its declaring class. Consumers at `0x3bc30` and
`0x3bdec` resolve the name and DEX descriptor from the next two words. This
allows the numbered `LibCustomApplication` calls in recovered Java methods to
be resolved to actual fields and methods, rather than interpreted from their
decompiler wrapper names.

Record 5's first DEX is a class skeleton with no field, method or prototype
tables and no class-data bodies. It is not the complete application DEX.
The native container reader at `0x79010` reads a two-entry DEX offset directory
from the container trailer: `0x8000` and `0`. The DEX at `0x8000` has 988 raw
class definitions, 14,241 method identifiers and real purchase method bodies.
Its declared length is 4,007,188 bytes. All class-data/code-item bounds and
instruction extents parse successfully. Its stored DEX SHA-1 and Adler-32 do
not match the recovered bytes; the protected container is not an ordinary
checksum-valid APK DEX.

For inspection, a derivative repairs only that DEX's SHA-1 and Adler-32 and
adds it as `classes2.dex` alongside the original base APK's resources and
`classes.dex`. This reconstructed APK is an analysis input; it has not been
signed, installed or treated as runnable. REA/JADX parses 8,248 top-level
classes from it. The preserved hashes distinguish each stage:

| Inspection artifact | SHA-256 |
| --- | --- |
| Recovered secondary DEX, original header | `66af08b8b74fef15e290fdb1eb1329937d696a5586bccf2ab9904c305af48adb` |
| Secondary DEX with analysis-only checksum repair | `774437a58cb0dd256ae52eac31cb801b16cb6a6259c116dad7afb9082174d7c8` |
| Expanded inspection APK | `6787433ca3e55b3116e795403e6078d1ced3f9a1279a3c0c534c2ae0e28ad5cf` |

The native disassembly and reproducible recovery scripts are retained in
`/private/tmp/kanji-study-research/`. Hopper became unreachable during the later
key routine analysis; subsequent work uses the recovered raw bytes and local
AArch64 disassembly. The later native conclusions above use analyst-reproduced
algorithms and `objdump` instruction listings, rather than an exported REA view.
The raw memory image, constructor addresses and instruction bytes are preserved
so those calculations can be repeated. The local authenticated directory
decoder also passes independent RFC 8439 ChaCha20 and Poly1305 vectors.

## Recovered purchase paths

These observations combine REA/JADX method bodies from the expanded inspection
APK with the native-confirmed member map. Short obfuscated names below are the
shipped DEX names; JADX displays some under `defpackage`.

| Path | Static observation |
| --- | --- |
| `r13.o8` → `r13.b()Z` | Upgrade state reads a per-setting key through `s13.d(String, boolean)`, which calls `SharedPreferences.getBoolean`. |
| `r13.v(Z)V` → `s13.z(String, boolean)` | The corresponding setter calls `SharedPreferences.edit`, `putBoolean` and `apply`. |
| `PurchaseUpgradeActivity.p()` | The upgrade boolean controls purchase-button opacity, price visibility and the owned-state label. |
| `KanjiModeActivity.n()` | A selected mode of `-1` is changed to `1` when that upgrade boolean is false. This is a feature consumer beyond the purchase screen. |
| `s13.H()` | A compound condition includes the inverse of this upgrade state and two other settings; it does not establish a universal premium flag. |
| `x3.o(BillingResult, List)` | A nonzero billing response emits a failure event. A matching product branch sets the upgrade preference and invokes a purchase-validation request path. Other product branches update distinct settings. |
| `gl4.b(String)` | The HTTP response callback requires status 200, parses a response status and two identity strings, compares both identities with request/purchase values, and requires response status zero before its success branch writes the upgrade state. |

The generic `r13.b()` getter serves many settings. Replacing its result
globally would change unrelated application preferences. These observations
also do not establish that all paid content or features depend on `r13.o8`.
The billing callback and HTTP callback both contain state writes, so the
inspected code does not support a claim that the server is the sole enforcement
point.

The response identity comparisons are concrete; their textual JSON keys and
product identifiers still use `ProtectedCustomApplication.s(...)`. The names
of those encrypted keys have not been recovered, so the compared values are
not classified here as a receipt, order identifier or purchase token. JADX's
billing-loop rendering also requires raw DEX control-flow confirmation before
any mutation is based on that method.

Retained REA evidence:
`ev_1afecdee899f9ec40fa188a53328f5a284aaaa9acbc6cf8b260e3c6751917b05`
(`r13.b`),
`ev_a42058b02e1010e5dd3cf5d3ff070716a6a0ee46aaaadd754c64d7d046f99e37`
(`s13.d`),
`ev_bc017d0bbec8cd0efb7108f9821b92dea4262ad92a8f4d1cf139c6223e67313c`
(`r13.v`),
`ev_8a4f6ba9418c146b97af6d110e35d52e232f9d624c18ec9fee87261f540969af`
(`s13.z`),
`ev_b44c01930a9425391500cfc8b6df0b6b43709702b3de93b03e477e734c14373a`
(`PurchaseUpgradeActivity.p`),
`ev_31fc16b74d5e7954cb2b6f1d560b801f4f4216885e568772a4d43724de552f94`
(`KanjiModeActivity.n`),
`ev_95643a31cfe2b68491214af0d8c40250919ac59ddbf670b9b84c519ac4b693ba`
(`s13.H`),
`ev_2696b7dc3f22088901947904f2130eec3f17f8e82eaf97db60164213273993a8`
(`x3.o`),
`ev_89e00f4b3f6debb8fbf903efa5a77c2b2e29fcdfd6bceb676312af39b2be3a2b`
(`gl4.b`) and
`ev_9ce215ada68711545439779f22e9dee747c4c0cb113ce314342d8efcb96bebd9`
(`zb2.b`, response-status comparison).

## Google paths and patch feasibility

`GoogleDriveActivity.l()` resolves member 9623 to
`GoogleAccountCredential.usingOAuth2(Context, Collection)` and member 18594 to
`setSelectedAccountName(String)`. It builds a Google Drive client with the
credential, `NetHttpTransport` and `GsonFactory`. This establishes an active
Google OAuth2 path. Its account selection, availability checks, auth-token
transport and behavior with MicroG have not been validated. The observed
`onActivityResult` file-URI handling alone is not a Google sign-in callback.
Evidence:
`ev_cadfcf903ffd1b5f8cb6378d609c36eb8a4fe4369b651319c9e0a82bfb4b4eb9`.
The earlier app-owned FCM preference entry point remains visible; registration
and delivery require a runtime test.

No premium or MicroG patch is included for this artifact. Required purchase
methods reside in encrypted asset records, outside the original base DEX that
the current patch bundle processes. The checksum-repaired inspection APK does
not reconstruct the protector's loader integration. Re-signing fails the Java
certificate check, and the native key derivation also mixes the original signer,
the first 1,024 bytes of `classes.dex` and normalized native code. A Java
startup-check override alone would not preserve those authenticated keys.

Static recovery has exposed purchase methods without source mappings or a
diagnostic build. A concrete next static step is to trace the record-1 string
decoder, recover the per-product preference/JSON names, and verify raw DEX
branches and additional content consumers. A deployable mutation then needs
the loader's protected-record rewriting and authentication to be reconstructed
and tested, or a developer test build that supports re-signing. Neither startup
nor purchase behavior has been tested for a modified Kanji Study APK.

Another recovery route is a controlled capture of the original signed app at
its asset-decryption and DEX-loading boundaries. That requires a working test
device and suitable debugging privileges and may encounter protector checks.
It has not been performed. The isolated Android emulator and loopback ADB now
work outside the sandbox, but suitable debugging privileges and a capture at
these protected boundaries have not been established. Other apps' startup
tests do not supply runtime evidence for Kanji Study.

Use free and entitled developer test accounts to compare upgrade, Guided Study,
KLC and Outlier consumers and server responses. For MicroG, verify the actual
account/token flow, availability checker, FCM registration and delivery on a
test device after establishing a supported test-signing path.

The private evidence ledgers, input verification, native snapshot, record
decoders, member parser and DEX structural checks are retained in
`/private/tmp/kanji-study-research/`. No APK, content database or decompiled app
source is included in this repository.
