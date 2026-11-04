plugins {
    `java-library`
    kotlin("plugin.serialization")
}

dependencies {
    // api, a ne implementation: ko koristi model, koristi i @Serializable klase
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
}
