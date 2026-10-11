# Bundled Kotlin libraries

All Kotlin and kotlinx artifacts below are redistributed as unmodified JVM JARs. Copyright JetBrains s.r.o. and contributors. Their main license is Apache License 2.0, included in [licenses/JetBrains-Apache-2.0.txt](licenses/JetBrains-Apache-2.0.txt). Upstream attribution notices are reproduced in [NOTICE](NOTICE).

| Artifacts | Version | Source |
| --- | --- | --- |
| kotlin-stdlib, kotlin-stdlib-jdk7, kotlin-stdlib-jdk8, kotlin-reflect | 2.4.0 | https://github.com/JetBrains/kotlin/tree/v2.4.0 |
| kotlinx-coroutines-core-jvm, kotlinx-coroutines-jdk8 | 1.11.0 | https://github.com/Kotlin/kotlinx.coroutines/tree/1.11.0 |
| kotlinx-serialization-core-jvm, kotlinx-serialization-json-jvm, kotlinx-serialization-cbor-jvm | 1.11.0 | https://github.com/Kotlin/kotlinx.serialization/tree/v1.11.0 |
| atomicfu-jvm | 0.33.0 | https://github.com/Kotlin/kotlinx-atomicfu/tree/0.33.0 |
| kotlinx-datetime-jvm | 0.8.0 | https://github.com/Kotlin/kotlinx-datetime/tree/v0.8.0 |
| kotlinx-io-core-jvm, kotlinx-io-bytestring-jvm | 0.9.1 | https://github.com/Kotlin/kotlinx-io/tree/0.9.1 |

Kotlin reflection contains Protocol Buffers code covered by [its BSD license](licenses/Protocol-Buffers-BSD.txt). Kotlin time and datetime calculations include code derived from ThreeTen Backport, covered by [BSD 3-Clause](licenses/ThreeTenBP-BSD.txt). Kotlin JVM math functions include Boost-derived code; its [license](licenses/Boost-1.0.txt) is also included. GWT-derived collections and Guava-derived unsigned operations are covered by Apache 2.0; their attribution is retained in NOTICE. These third-party terms remain separate from the LGPL license of the Kotlin Reforged adapters.

Exact download URLs and SHA-512 digests are recorded in gradle/kotlin-runtime.json and gradle/additional-kotlin-libraries.json at the repository root.
