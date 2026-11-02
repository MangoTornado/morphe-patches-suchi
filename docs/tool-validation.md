# Research tool validation

Verified on 2026-11-02 using author-owned ARM64/DEX controls, authored parser buffers and published SDK source. These trials establish which tools and dependency contracts work locally. Application startup and protected VM behavior remain unverified.

| Tool or dependency | Verified result | Limit |
| --- | --- | --- |
| Ghidra 12.1.4 | 20 baseline p-code cases passed and five exports decompiled. A separate JNI trial passed seven cases, matching unidbg return values. REA 6.7.0 CLI output matched the baseline. | JNI callbacks are explicit models; the null-string case overlaps the baseline. No Android runtime was executed. |
| unidbg 0.9.9 | 33 arithmetic/JNI checks passed with legacy Unicorn. | Unicorn2 failed during initialization. Unsupported JNI callbacks can throw internally while returning `-1` externally. |
| Androguard 4.1.4 | Three owned DEX inventories matched Android SDK `dexdump`. | Metadata extraction does not establish runtime behavior. |
| `MatrixEditor/pairipcore-vm` private repaired fork | 214 correctness regressions passed; the identical corpus failed 131 checks on pinned upstream. | Repairs cover operand strides, instruction length, custom decoder propagation and handler stores. No application opcodes were recovered. |
| AndroidX WorkManager 2.9.0 | 45 STOP, restart, teardown and routing cases passed against nine unchanged public methods. | Framework, initialization, coroutine and scheduler dependencies are fixtures. Application field mapping is unknown. |
| JEB demo 5.48.0 | Normal activation and core initialization completed. | No fixture analysis completed. Public docs place automation behind edition/capability requirements; the silent API exit remains unexplained. |

The Ghidra JNI control checks null/empty strings, UTF-16 lengths, a modeled static callback and the missing-method branch. JNI slots were derived from NDK 27.1.12297006 headers for ARM64/API 24. An unmodeled callback stops the trial instead of producing a successful result.

The parser fork preserves upstream commit `38c823cc08d40c2384451feffac1ed09a68e9f40`. Its tests cover 21 authored format combinations, a supplied address decoder, typed stores, checksum branches and interpreter dispatch. The original 37-check bug-reproduction baseline remains unchanged. The 214 regressions test corrected behavior; malformed inputs, arbitrary layouts and compatibility with an application's VM remain untested.

WorkManager's dispatcher forwards STOP to its callback. Service STOP marks shutdown, calls `stopForeground(true)` from API 26, clears its source singleton and calls `stopSelf`. Dispatcher teardown cancels tracked jobs while holding its lock, then removes its listener. Restart tears down and initializes before forwarding a command; `onCreate` assigns the singleton. These source contracts do not supply the optimized application's missing singleton or lifecycle mapping.

Provenance is pinned to the owned native fixture SHA-256 `280853513ecf617ee65bf8f76a6911ef1123635ae2af81381286bc26146105ed` and the WorkManager source JAR SHA-256 `9565d9b68b42a558344e08be1909dbed331e0060ceba66cccd1ca26a6c7b7a5a`. Sources, fixtures, harnesses and reports remain private under `.research-tools/tool-trials/`; no APK or bundle was produced by these trials.
